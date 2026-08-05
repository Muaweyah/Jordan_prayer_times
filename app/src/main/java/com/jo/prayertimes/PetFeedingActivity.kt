package com.jo.prayertimes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PetFeedingActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var manager: PetFeedingManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: PetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_feeding)

        HomeNavigator.wire(this)
        manager = PetFeedingManager(this)

        recyclerView = findViewById(R.id.rvPets)
        tvEmpty = findViewById(R.id.tvEmptyPets)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PetAdapter(
            mutableListOf(),
            onItemClick = { pet -> openDetail(pet.id) },
            onDeleteClick = { pet -> confirmDelete(pet) }
        )
        recyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.btnAddPet).setOnClickListener {
            openDetail(null)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val pets = manager.getPets()
        adapter.updateData(pets)
        tvEmpty.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openDetail(petId: Int?) {
        val intent = Intent(this, PetDetailActivity::class.java)
        if (petId != null) intent.putExtra(PetDetailActivity.EXTRA_PET_ID, petId)
        startActivity(intent)
    }

    private fun confirmDelete(pet: Pet) {
        AlertDialog.Builder(this)
            .setTitle("حذف ${pet.name}")
            .setMessage("سيتم حذف بيانات ${pet.name} وكل تنبيهات إطعامه. متابعة؟")
            .setPositiveButton("حذف") { _, _ ->
                val scheduler = PetFeedingScheduler(this)
                for (meal in pet.meals) {
                    scheduler.cancelMeal(meal.id)
                }
                manager.deletePet(pet.id)
                refreshList()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
