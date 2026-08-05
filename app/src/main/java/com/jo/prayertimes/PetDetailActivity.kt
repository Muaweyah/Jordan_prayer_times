package com.jo.prayertimes

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class PetDetailActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var manager: PetFeedingManager
    private lateinit var etName: EditText
    private lateinit var spType: Spinner
    private lateinit var btnSound: Button
    private lateinit var llMeals: LinearLayout
    private lateinit var btnDeletePet: Button

    private var petId: Int = -1
    private var selectedSoundUri: String? = null
    private val meals = mutableListOf<PetMeal>()
    private var originalMealIds: Set<Int> = emptySet()
    private var nextTempMealId = -1 // معرّفات مؤقتة سالبة للوجبات الجديدة، تُستبدل بمعرّف نهائي عند الحفظ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)
        HomeNavigator.wire(this)

        manager = PetFeedingManager(this)
        etName = findViewById(R.id.etPetName)
        spType = findViewById(R.id.spPetType)
        btnSound = findViewById(R.id.btnPickSound)
        llMeals = findViewById(R.id.llMeals)
        btnDeletePet = findViewById(R.id.btnDeletePet)

        val typeAdapter = ArrayAdapter(this, R.layout.spinner_item, PetType.values().map { it.arabicLabel })
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spType.adapter = typeAdapter

        petId = intent.getIntExtra(EXTRA_PET_ID, -1)
        if (petId != -1) {
            val pet = manager.getPet(petId)
            if (pet != null) {
                etName.setText(pet.name)
                spType.setSelection(PetType.values().indexOf(pet.type).coerceAtLeast(0))
                selectedSoundUri = pet.soundUri
                meals.addAll(pet.meals.map { it.copy() })
                originalMealIds = pet.meals.map { it.id }.toSet()
                btnDeletePet.visibility = View.VISIBLE
            }
        }
        updateSoundButtonLabel()
        renderMeals()

        btnSound.setOnClickListener { pickSound() }
        findViewById<Button>(R.id.btnAddMeal).setOnClickListener { pickMealTime() }
        findViewById<Button>(R.id.btnSavePet).setOnClickListener { savePet() }
        btnDeletePet.setOnClickListener { deletePet() }
    }

    private fun pickSound() {
        val current = selectedSoundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة تنبيه الإطعام")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
        }
        startActivityForResult(intent, REQUEST_CODE_SOUND)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SOUND) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            selectedSoundUri = uri?.toString()
            updateSoundButtonLabel()
        }
    }

    private fun updateSoundButtonLabel() {
        val uri = selectedSoundUri
        btnSound.text = if (uri != null) {
            val title = try {
                RingtoneManager.getRingtone(this, Uri.parse(uri))?.getTitle(this)
            } catch (e: Exception) {
                null
            }
            "النغمة: ${title ?: "مخصّصة"}"
        } else {
            "النغمة: افتراضية"
        }
    }

    private fun pickMealTime() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                meals.add(PetMeal(id = nextTempMealId--, hour = hour, minute = minute, enabled = true))
                renderMeals()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun renderMeals() {
        llMeals.removeAllViews()
        if (meals.isEmpty()) {
            val empty = TextView(this).apply {
                text = "لم تُضف أي وجبة بعد. اضغط \"إضافة وجبة\" لتحديد أول موعد."
                setTextColor(getColorCompat(R.color.text_secondary))
                setPadding(4, 16, 4, 16)
            }
            llMeals.addView(empty)
            return
        }
        val inflater = LayoutInflater.from(this)
        for (meal in meals.sortedBy { it.hour * 60 + it.minute }) {
            val row = inflater.inflate(R.layout.item_meal_row, llMeals, false)
            val tvTime = row.findViewById<TextView>(R.id.tvMealTime)
            val swEnabled = row.findViewById<Switch>(R.id.swMealEnabled)
            val btnDelete = row.findViewById<ImageButton>(R.id.btnDeleteMeal)

            tvTime.text = String.format("%02d:%02d", meal.hour, meal.minute)
            swEnabled.isChecked = meal.enabled
            swEnabled.setOnCheckedChangeListener { _, isChecked -> meal.enabled = isChecked }
            tvTime.setOnClickListener {
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        meal.hour = hour
                        meal.minute = minute
                        renderMeals()
                    },
                    meal.hour, meal.minute, true
                ).show()
            }
            btnDelete.setOnClickListener {
                meals.remove(meal)
                renderMeals()
            }
            llMeals.addView(row)
        }
    }

    private fun getColorCompat(colorRes: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) resources.getColor(colorRes, theme)
        else @Suppress("DEPRECATION") resources.getColor(colorRes)

    private fun savePet() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "الرجاء إدخال اسم الحيوان الأليف", Toast.LENGTH_SHORT).show()
            return
        }
        if (meals.isEmpty()) {
            Toast.makeText(this, "أضف وجبة واحدة على الأقل", Toast.LENGTH_SHORT).show()
            return
        }

        val type = PetType.values()[spType.selectedItemPosition]
        val scheduler = PetFeedingScheduler(this)

        // إلغاء تنبيهات أي وجبات كانت موجودة سابقاً وحُذفت في هذه الجلسة قبل الحفظ
        val currentIds = meals.filter { it.id > 0 }.map { it.id }.toSet()
        for (removedId in originalMealIds - currentIds) {
            scheduler.cancelMeal(removedId)
        }

        // منح معرّفات نهائية دائمة للوجبات الجديدة (كانت مؤقتة بأرقام سالبة)
        val finalMeals = meals.map { meal ->
            if (meal.id < 0) meal.copy(id = manager.nextId()) else meal
        }.toMutableList()

        val id = if (petId != -1) petId else manager.nextId()
        val pet = Pet(id = id, name = name, type = type, soundUri = selectedSoundUri, meals = finalMeals)
        manager.addOrUpdatePet(pet)
        scheduler.rescheduleAll()

        Toast.makeText(this, "تم حفظ ${pet.name}", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deletePet() {
        AlertDialog.Builder(this)
            .setTitle("حذف الحيوان الأليف")
            .setMessage("سيتم حذف كل بيانات وتنبيهات هذا الحيوان الأليف. متابعة؟")
            .setPositiveButton("حذف") { _, _ ->
                val scheduler = PetFeedingScheduler(this)
                for (meal in meals) {
                    if (meal.id > 0) scheduler.cancelMeal(meal.id)
                }
                manager.deletePet(petId)
                finish()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    companion object {
        const val EXTRA_PET_ID = "extra_pet_id"
        private const val REQUEST_CODE_SOUND = 4001
    }
}
