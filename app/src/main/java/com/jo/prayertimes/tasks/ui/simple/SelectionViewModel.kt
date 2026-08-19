package com.jo.prayertimes.tasks.ui.simple

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jo.prayertimes.tasks.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TasksDatabase.getInstance(application)

    val selected: StateFlow<List<SelectedTask>> = db.selectedTaskDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTemplate(title: String, categoryId: String) {
        viewModelScope.launch {
            val existing = db.selectedTaskDao().find(title, categoryId)
            if (existing != null) {
                db.selectedTaskDao().delete(existing)
            } else {
                db.selectedTaskDao().insert(SelectedTask(title = title, categoryId = categoryId))
            }
        }
    }

    fun addCustom(title: String, categoryId: String) {
        viewModelScope.launch {
            db.selectedTaskDao().insert(SelectedTask(title = title, categoryId = categoryId, isCustom = true))
        }
    }

    fun removeCustom(item: SelectedTask) {
        viewModelScope.launch { db.selectedTaskDao().delete(item) }
    }

    fun resetAllData() {
        viewModelScope.launch {
            db.selectedTaskDao().deleteAll()
            db.dailyLogDao().deleteAll()
        }
    }
}
