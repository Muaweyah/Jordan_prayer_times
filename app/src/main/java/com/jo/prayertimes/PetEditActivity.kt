package com.jo.prayertimes

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

/** شاشة إضافة حيوان أليف جديد أو تعديل بياناته: الاسم، النوع، نغمة التنبيه، ومواعيد وجباته المتعددة */
class PetEditActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var repository: PetRepository
    private lateinit var etPetName: EditText
    private lateinit var spPetType: Spinner
    private lateinit var btnChooseSound: Button
    private lateinit var mealsContainer: LinearLayout
    private lateinit var btnDeletePet: Button

    private var petId: String = ""
    private var selectedSoundUri: String? = null
    /** كل صف وجبة معروض حالياً على الشاشة مع بيانات الوجبة المرتبطة به */
    private val mealRows = mutableListOf<Pair<View, PetMeal>>()

    private val petTypesOrdered = PetType.values().toList()

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        selectedSoundUri = uri?.toString()
        updateSoundButtonText()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_edit)

        HomeNavigator.wire(this)
        repository = PetRepository(this)

        etPetName = findViewById(R.id.etPetName)
        spPetType = findViewById(R.id.spPetType)
        btnChooseSound = findViewById(R.id.btnChooseSound)
        mealsContainer = findViewById(R.id.mealsContainer)
        val tvTitle = findViewById<TextView>(R.id.tvPetEditTitle)
        val btnAddMeal = findViewById<Button>(R.id.btnAddMeal)
        val btnSavePet = findViewById<Button>(R.id.btnSavePet)
        btnDeletePet = findViewById(R.id.btnDeletePet)

        val typeLabels = petTypesOrdered.map { "${it.emoji} ${localizedTypeLabel(it)}" }
        val typeAdapter = ArrayAdapter(this, R.layout.spinner_item, typeLabels)
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spPetType.adapter = typeAdapter

        val existingPetId = intent.getStringExtra(EXTRA_PET_ID)
        val existingPet = existingPetId?.let { repository.getPet(it) }

        if (existingPet != null) {
            petId = existingPet.id
            tvTitle.text = existingPet.name
            etPetName.setText(existingPet.name)
            spPetType.setSelection(petTypesOrdered.indexOf(existingPet.type).takeIf { it >= 0 } ?: 0)
            selectedSoundUri = existingPet.soundUri
            existingPet.meals.forEach { addMealRow(it) }
            btnDeletePet.visibility = View.VISIBLE
            btnDeletePet.setOnClickListener { confirmDeletePet(existingPet) }
        } else {
            petId = UUID.randomUUID().toString()
            tvTitle.text = getString(R.string.add_pet)
        }
        updateSoundButtonText()

        btnChooseSound.setOnClickListener { launchSoundPicker() }

        btnAddMeal.setOnClickListener {
            addMealRow(PetMeal(id = UUID.randomUUID().toString(), hour = 8, minute = 0))
        }

        btnSavePet.setOnClickListener { savePet() }
    }

    private fun localizedTypeLabel(type: PetType): String = when (type) {
        PetType.CAT -> getString(R.string.pet_type_cat)
        PetType.DOG -> getString(R.string.pet_type_dog)
        PetType.BIRD -> getString(R.string.pet_type_bird)
        PetType.FISH -> getString(R.string.pet_type_fish)
        PetType.RABBIT -> getString(R.string.pet_type_rabbit)
        PetType.OTHER -> getString(R.string.pet_type_other)
    }

    private fun launchSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            val currentUri = selectedSoundUri?.let { Uri.parse(it) }
            if (currentUri != null) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            }
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun updateSoundButtonText() {
        val soundName = selectedSoundUri?.let { uriString ->
            try {
                RingtoneManager.getRingtone(this, Uri.parse(uriString))?.getTitle(this)
            } catch (e: Exception) {
                null
            }
        } ?: getString(R.string.sound_default_label)
        btnChooseSound.text = getString(R.string.sound_chosen_format, soundName)
    }

    private fun addMealRow(meal: PetMeal) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_pet_meal_row, mealsContainer, false)
        val etLabel = row.findViewById<EditText>(R.id.etMealLabel)
        val btnTime = row.findViewById<Button>(R.id.btnMealTime)
        val swEnabled = row.findViewById<Switch>(R.id.swMealEnabled)
        val btnRemove = row.findViewById<android.widget.ImageButton>(R.id.btnRemoveMeal)

        etLabel.setText(meal.label)
        swEnabled.isChecked = meal.enabled
        updateMealTimeButtonText(btnTime, meal)

        btnTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    meal.hour = hour
                    meal.minute = minute
                    updateMealTimeButtonText(btnTime, meal)
                },
                meal.hour,
                meal.minute,
                true
            ).show()
        }

        swEnabled.setOnCheckedChangeListener { _, isChecked -> meal.enabled = isChecked }

        btnRemove.setOnClickListener {
            mealsContainer.removeView(row)
            mealRows.removeAll { it.first == row }
        }

        mealsContainer.addView(row)
        mealRows.add(row to meal)
    }

    private fun updateMealTimeButtonText(button: Button, meal: PetMeal) {
        button.text = String.format("%02d:%02d", meal.hour, meal.minute)
    }

    private fun savePet() {
        val name = etPetName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.pet_name_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (mealRows.isEmpty()) {
            Toast.makeText(this, getString(R.string.meal_required), Toast.LENGTH_SHORT).show()
            return
        }

        // نقرأ اسم كل وجبة من حقل الإدخال الخاص بها مباشرة قبل التجميع، لأنها لا تُحدَّث تلقائياً داخل الكائن
        val finalMeals = mealRows.map { (view, meal) ->
            meal.label = view.findViewById<EditText>(R.id.etMealLabel).text.toString().trim()
            meal
        }.toMutableList()

        val selectedType = petTypesOrdered[spPetType.selectedItemPosition]
        val pet = Pet(
            id = petId,
            name = name,
            type = selectedType,
            soundUri = selectedSoundUri,
            meals = finalMeals
        )

        // نُلغي أولاً أي تنبيهات قديمة لهذا الحيوان (بمعرفات وجبات قد تكون حُذفت أو تغيّرت) قبل حفظ البيانات الجديدة وإعادة الجدولة
        repository.getPet(petId)?.let { PetAlarmScheduler(this).cancelForPet(it) }
        repository.upsertPet(pet)
        PetAlarmScheduler(this).rescheduleAll()

        Toast.makeText(this, getString(R.string.pet_saved_format, name), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmDeletePet(pet: Pet) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_pet_title)
            .setMessage(getString(R.string.confirm_delete_pet_message, pet.name))
            .setPositiveButton(R.string.delete_confirm_label) { _, _ ->
                PetAlarmScheduler(this).cancelForPet(pet)
                PetNotificationHelper.deleteAllChannelsForPet(this, pet.id)
                repository.deletePet(pet.id)
                Toast.makeText(this, getString(R.string.pet_deleted_format, pet.name), Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(R.string.cancel_label, null)
            .show()
    }

    companion object {
        const val EXTRA_PET_ID = "extra_pet_id"
    }
}
