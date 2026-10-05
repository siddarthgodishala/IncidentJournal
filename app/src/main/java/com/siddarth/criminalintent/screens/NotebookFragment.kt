package com.siddarth.criminalintent.screens

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.*
import com.siddarth.criminalintent.R
import androidx.navigation.fragment.findNavController
import com.siddarth.criminalintent.databinding.FragmentNotebookBinding
import com.siddarth.criminalintent.databinding.RowIncidentBinding
import com.siddarth.criminalintent.storage.Incident
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

class NotebookFragment : Fragment(R.layout.fragment_notebook) {
    private val model: NotebookModel by viewModels()
    private fun openEntry(key: String? = null) {
        findNavController().navigate(NotebookFragmentDirections.openEntry(key))
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        val ui = FragmentNotebookBinding.bind(view)
        val rows = NotebookRows { openEntry(it.key) }
        ui.entries.layoutManager = LinearLayoutManager(requireContext())
        ui.entries.adapter = rows
        ui.listToolbar.inflateMenu(R.menu.notebook_actions)
        tintNotebookToolbar(ui.listToolbar)
        ui.listToolbar.setOnMenuItemClickListener { openEntry(); true }
        ui.newEntry.setOnClickListener { openEntry() }
        ui.emptyNew.setOnClickListener { openEntry() }
        ui.search.setText(model.query.value)
        ui.search.doAfterTextChanged { model.search(it.toString()) }
        ui.statusFilter.check(when (model.filter.value) { "open" -> R.id.filter_open; "solved" -> R.id.filter_solved; else -> R.id.filter_all })
        ui.statusFilter.setOnCheckedStateChangeListener { _, ids ->
            model.filter(when(ids.firstOrNull()) { R.id.filter_open -> "open"; R.id.filter_solved -> "solved"; else -> "all" })
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.page.collect { page ->
                    ui.loading.isVisible = !page.ready
                    ui.totals.text = getString(R.string.count, page.total, page.open)
                    ui.emptyPanel.isVisible = page.ready && page.records.isEmpty()
                    ui.emptyNew.isVisible = page.total == 0 && !page.failed
                    ui.emptyMessage.setText(when { page.failed -> R.string.load_error; page.total == 0 -> R.string.empty; else -> R.string.no_matches })
                    rows.submitList(page.records)
                }
            }
        }
    }
}

private class NotebookRows(private val open: (Incident) -> Unit) : ListAdapter<Incident, NotebookRows.Row>(object : DiffUtil.ItemCallback<Incident>() {
    override fun areItemsTheSame(a: Incident, b: Incident) = a.key == b.key
    override fun areContentsTheSame(a: Incident, b: Incident) = a == b
}) {
    init { stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY }
    class Row(val ui: RowIncidentBinding) : RecyclerView.ViewHolder(ui.root)
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Row(RowIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(row: Row, position: Int) {
        val item = getItem(position)
        row.ui.rowTitle.text = item.heading
        row.ui.rowDate.text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.occurredAt))
        row.ui.rowStatus.setText(if (item.resolved) R.string.solved else R.string.open)
        row.ui.root.setOnClickListener { open(item) }
    }
}
