package com.jo.prayertimes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PetAdapter(
    private var pets: MutableList<Pet>,
    private val onItemClick: (Pet) -> Unit,
    private val onDeleteClick: (Pet) -> Unit
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    class PetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.rowPetItem)
        val icon: ImageView = view.findViewById(R.id.ivPetIcon)
        val name: TextView = view.findViewById(R.id.tvPetName)
        val subtitle: TextView = view.findViewById(R.id.tvPetSubtitle)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeletePet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]
        holder.name.text = pet.name
        holder.icon.setImageResource(R.drawable.ic_paw)

        val enabledMeals = pet.meals.count { it.enabled }
        holder.subtitle.text = if (enabledMeals > 0) {
            "${pet.type.arabicLabel} • $enabledMeals وجبة يومياً"
        } else {
            "${pet.type.arabicLabel} • لا توجد وجبات مفعّلة"
        }

        holder.root.setOnClickListener { onItemClick(pet) }
        holder.btnDelete.setOnClickListener { onDeleteClick(pet) }
    }

    override fun getItemCount(): Int = pets.size

    fun updateData(newList: List<Pet>) {
        pets = newList.toMutableList()
        notifyDataSetChanged()
    }
}
