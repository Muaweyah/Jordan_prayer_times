package com.jo.prayertimes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** الشاشة الرئيسية لميزة "أطعم أليفك": تعرض كل الحيوانات الأليفة المضافة مع ملخّص مواعيد إطعامها */
class PetFeederActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var repository: PetRepository
    private lateinit var adapter: PetAdapter
    private lateinit var rvPets: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_feeder)

        HomeNavigator.wire(this)
        repository = PetRepository(this)

        rvPets = findViewById(R.id.rvPets)
        tvEmpty = findViewById(R.id.tvPetFeederEmpty)
        val btnAddPet = findViewById<Button>(R.id.btnAddPet)

        rvPets.layoutManager = LinearLayoutManager(this)
        adapter = PetAdapter(this, emptyList()) { pet ->
            startActivity(
                Intent(this, PetEditActivity::class.java).putExtra(PetEditActivity.EXTRA_PET_ID, pet.id)
            )
        }
        rvPets.adapter = adapter

        btnAddPet.setOnClickListener {
            startActivity(Intent(this, PetEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val pets = repository.getAllPets()
        adapter.updateData(pets)
        val isEmpty = pets.isEmpty()
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvPets.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
