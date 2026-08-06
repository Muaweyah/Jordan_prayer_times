package com.jo.prayertimes

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

/** يدير تسجيل الدخول بحساب جوجل وطلب صلاحية الوصول لمساحة بيانات التطبيق على Drive (appDataFolder).
 *  appDataFolder مساحة خاصة بالتطبيق نفسه داخل حساب المستخدم، غير ظاهرة له بتطبيق Drive العادي. */
class AuthManager(private val context: Context) {

    companion object {
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val RC_SIGN_IN = 5001
    }

    private val signInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(GoogleAuthConfig.WEB_CLIENT_ID)
            .requestScopes(Scope(SCOPE_DRIVE_APPDATA))
            .build()
    }

    private val client: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, signInOptions)
    }

    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun buildSignInIntent(): Intent = client.signInIntent

    fun signOut(onComplete: () -> Unit) {
        client.signOut().addOnCompleteListener { onComplete() }
    }

    /** يجب استدعاؤها من خيط خلفي فقط (ليست بالخيط الرئيسي) لأنها تنفّذ اتصال شبكة داخلياً */
    @Throws(Exception::class)
    fun fetchAccessToken(account: GoogleSignInAccount): String {
        val googleAccount = Account(account.email, "com.google")
        return GoogleAuthUtil.getToken(context, googleAccount, "oauth2:$SCOPE_DRIVE_APPDATA")
    }
}
