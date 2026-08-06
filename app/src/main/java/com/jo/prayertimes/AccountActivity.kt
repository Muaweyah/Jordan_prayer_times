package com.jo.prayertimes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class AccountActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var authManager: AuthManager
    private val driveSync = DriveSyncManager()
    private lateinit var backupManager: BackupManager

    private lateinit var tvStatus: TextView
    private lateinit var btnSignIn: Button
    private lateinit var btnSignOut: Button
    private lateinit var btnSyncUp: Button
    private lateinit var btnSyncDown: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        HomeNavigator.wire(this)

        authManager = AuthManager(this)
        backupManager = BackupManager(this)

        tvStatus = findViewById(R.id.tvAccountStatus)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignOut = findViewById(R.id.btnSignOut)
        btnSyncUp = findViewById(R.id.btnSyncUp)
        btnSyncDown = findViewById(R.id.btnSyncDown)

        btnSignIn.setOnClickListener { startActivityForResult(authManager.buildSignInIntent(), AuthManager.RC_SIGN_IN) }
        btnSignOut.setOnClickListener { signOut() }
        btnSyncUp.setOnClickListener { syncUp() }
        btnSyncDown.setOnClickListener { confirmSyncDown() }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val account = authManager.getSignedInAccount()
        val signedIn = account != null
        tvStatus.text = if (signedIn) {
            "مسجّل الدخول: ${account?.email}"
        } else {
            "لست مسجّل الدخول حالياً"
        }
        btnSignIn.visibility = if (signedIn) View.GONE else View.VISIBLE
        btnSignOut.visibility = if (signedIn) View.VISIBLE else View.GONE
        btnSyncUp.visibility = if (signedIn) View.VISIBLE else View.GONE
        btnSyncDown.visibility = if (signedIn) View.VISIBLE else View.GONE
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthManager.RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                task.getResult(ApiException::class.java)
                Toast.makeText(this, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Toast.makeText(this, "تعذّر تسجيل الدخول: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
            refreshUi()
        } else if (requestCode == RC_AUTH_RECOVERY) {
            // بعد موافقة المستخدم على صلاحية Drive، أعد محاولة نفس العملية المعلّقة
            if (resultCode == RESULT_OK) {
                when (pendingActionAfterRecovery) {
                    PendingAction.SYNC_UP -> syncUp()
                    PendingAction.SYNC_DOWN -> performSyncDown()
                    null -> {}
                }
            } else {
                Toast.makeText(this, "تم إلغاء منح صلاحية Drive", Toast.LENGTH_SHORT).show()
            }
            pendingActionAfterRecovery = null
        }
    }

    private fun signOut() {
        authManager.signOut {
            runOnUiThread {
                Toast.makeText(this, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                refreshUi()
            }
        }
    }

    private enum class PendingAction { SYNC_UP, SYNC_DOWN }
    private var pendingActionAfterRecovery: PendingAction? = null

    private fun syncUp() {
        val account = authManager.getSignedInAccount() ?: return
        setButtonsEnabled(false)
        Toast.makeText(this, "جاري رفع البيانات لـ Drive...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val token = authManager.fetchAccessToken(account)
                val json = backupManager.buildBackupJson()
                driveSync.uploadBackup(token, json)
                runOnUiThread {
                    Toast.makeText(this, "تمت المزامنة بنجاح", Toast.LENGTH_SHORT).show()
                    setButtonsEnabled(true)
                }
            } catch (e: UserRecoverableAuthException) {
                pendingActionAfterRecovery = PendingAction.SYNC_UP
                runOnUiThread {
                    setButtonsEnabled(true)
                    startActivityForResult(e.intent, RC_AUTH_RECOVERY)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "فشلت المزامنة: ${e.message}", Toast.LENGTH_LONG).show()
                    setButtonsEnabled(true)
                }
            }
        }.start()
    }

    private fun confirmSyncDown() {
        AlertDialog.Builder(this)
            .setTitle("استرجاع من Drive")
            .setMessage("سيتم استبدال كل بيانات التطبيق الحالية (الإعدادات، الحيوانات الأليفة) بآخر نسخة محفوظة على Drive. متابعة؟")
            .setPositiveButton("استرجاع") { _, _ -> performSyncDown() }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun performSyncDown() {
        val account = authManager.getSignedInAccount() ?: return
        setButtonsEnabled(false)
        Toast.makeText(this, "جاري الاسترجاع من Drive...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val token = authManager.fetchAccessToken(account)
                val json = driveSync.downloadBackup(token)
                if (json == null) {
                    runOnUiThread {
                        Toast.makeText(this, "لا توجد نسخة محفوظة على Drive بعد", Toast.LENGTH_LONG).show()
                        setButtonsEnabled(true)
                    }
                    return@Thread
                }
                backupManager.restoreFromJson(json)
                PetFeedingScheduler(this).rescheduleAll()
                runOnUiThread {
                    Toast.makeText(this, "تم الاسترجاع بنجاح، أعد فتح التطبيق لتطبيق كل التغييرات", Toast.LENGTH_LONG).show()
                    setButtonsEnabled(true)
                }
            } catch (e: UserRecoverableAuthException) {
                pendingActionAfterRecovery = PendingAction.SYNC_DOWN
                runOnUiThread {
                    setButtonsEnabled(true)
                    startActivityForResult(e.intent, RC_AUTH_RECOVERY)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "فشل الاسترجاع: ${e.message}", Toast.LENGTH_LONG).show()
                    setButtonsEnabled(true)
                }
            }
        }.start()
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnSyncUp.isEnabled = enabled
        btnSyncDown.isEnabled = enabled
        btnSignOut.isEnabled = enabled
    }

    companion object {
        private const val RC_AUTH_RECOVERY = 5002
    }
}
