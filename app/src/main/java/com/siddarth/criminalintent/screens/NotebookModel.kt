package com.siddarth.criminalintent.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.siddarth.criminalintent.NotebookApplication
import com.siddarth.criminalintent.storage.Incident
import kotlinx.coroutines.flow.*

data class NotebookPage(val records: List<Incident> = emptyList(), val total: Int = 0, val open: Int = 0, val ready: Boolean = false, val failed: Boolean = false)

class NotebookModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    val query = saved.getStateFlow("query", "")
    val filter = saved.getStateFlow("filter", "all")
    val page = combine((app as NotebookApplication).store.incidents().watch(), query, filter) { records, query, filter ->
        val visible = records.filter { item ->
            (item.heading.contains(query.trim(), ignoreCase = true) || item.notes.contains(query.trim(), ignoreCase = true)) && when (filter) {
                "open" -> !item.resolved
                "solved" -> item.resolved
                else -> true
            }
        }
        NotebookPage(visible, records.size, records.count { !it.resolved }, true)
    }.catch { emit(NotebookPage(ready = true, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotebookPage())
    fun search(value: String) { saved["query"] = value }
    fun filter(value: String) { saved["filter"] = value }
}
