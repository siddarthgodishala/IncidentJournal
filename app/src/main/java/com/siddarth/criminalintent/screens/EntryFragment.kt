package com.siddarth.criminalintent.screens

import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.siddarth.criminalintent.R
import com.siddarth.criminalintent.databinding.FragmentEntryBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class EntryFragment : Fragment(R.layout.fragment_entry) {
    private val model: EntryModel by viewModels()
    private var ui: FragmentEntryBinding? = null
    private var painting = false
    private var photoTask: Job? = null
    private var shownPhoto: String? = null
    private var initialized = false
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        model.receivePhoto(success) { message(R.string.photo_error) }
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        val binding = FragmentEntryBinding.bind(view)
        ui = binding
        binding.editorToolbar.setTitle(if (model.existing) R.string.edit_title else R.string.new_title)
        binding.editorToolbar.navigationIcon = DrawerArrowDrawable(requireContext()).apply { progress = 1f }
        binding.editorToolbar.setNavigationContentDescription(R.string.back)
        binding.editorToolbar.setNavigationOnClickListener { leave() }
        binding.editorToolbar.inflateMenu(R.menu.entry_actions)
        tintNotebookToolbar(binding.editorToolbar)
        binding.editorToolbar.menu.findItem(R.id.action_delete).isVisible = model.existing
        binding.editorToolbar.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.action_save -> {
                    if (model.ready.value) {
                        if (model.draft.heading.isBlank()) { binding.titleBox.error = getString(R.string.title_required); binding.entryTitle.requestFocus() }
                        else model.save { message(R.string.save_error) }
                    }
                }
                R.id.action_delete -> confirm(true)
            }; true
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() = leave() })
        binding.entryTitle.doAfterTextChanged { if (!painting) { binding.titleBox.error = null; model.change { item -> item.copy(heading = it.toString()) } } }
        binding.entryNotes.doAfterTextChanged { if (!painting) model.change { item -> item.copy(notes = it.toString()) } }
        binding.entrySolved.setOnCheckedChangeListener { _, checked -> if (!painting) model.change { it.copy(resolved = checked) } }
        binding.pickDate.setOnClickListener { pick(false) }
        binding.pickTime.setOnClickListener { pick(true) }
        binding.capture.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val destination = model.beginPhoto() ?: return@launch
                    val uri = FileProvider.getUriForFile(
                        requireContext(), "${requireContext().packageName}.photos", destination
                    )
                    camera.launch(uri)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    model.receivePhoto(false)
                    message(R.string.camera_error)
                }
            }
        }
        binding.sendReport.setOnClickListener { share() }
        childFragmentManager.setFragmentResultListener("timestamp", viewLifecycleOwner) { _, b -> model.change { it.copy(occurredAt = b.getLong("value")) } }
        childFragmentManager.setFragmentResultListener("discard", viewLifecycleOwner) { _, _ -> model.discard() }
        childFragmentManager.setFragmentResultListener("remove", viewLifecycleOwner) { _, _ -> model.remove { message(R.string.save_error) } }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.ready.collect { render() } }
                launch { model.revision.collect { render() } }
                launch { model.busy.collect { render() } }
                launch { model.photoPending.collect { render() } }
                launch { model.problem.collect { if (it) message(R.string.load_error) } }
                launch { model.closed.collect { if (it) findNavController().popBackStack() } }
            }
        }
        render()
    }

    private fun render() {
        val b = ui ?: return
        val enabled = model.ready.value && !model.busy.value && !model.photoPending.value
        listOf(b.entryTitle, b.entryNotes, b.entrySolved, b.pickDate, b.pickTime, b.capture, b.sendReport).forEach { it.isEnabled = enabled }
        b.editorToolbar.menu.findItem(R.id.action_save).isEnabled = enabled
        b.editorToolbar.menu.findItem(R.id.action_delete).isEnabled = enabled
        if (!model.ready.value) return
        val item = model.draft
        painting = true
        if (b.entryTitle.text.toString() != item.heading) b.entryTitle.setText(item.heading)
        if (b.entryNotes.text.toString() != item.notes) b.entryNotes.setText(item.notes)
        b.entrySolved.isChecked = item.resolved
        b.pickDate.text = DateFormat.getDateInstance(DateFormat.LONG).format(Date(item.occurredAt))
        b.pickTime.text = android.text.format.DateFormat.getTimeFormat(requireContext()).format(Date(item.occurredAt))
        painting = false
        if (!initialized || shownPhoto != item.image) {
            initialized = true
            shownPhoto = item.image
            photoTask?.cancel()
            photoTask = viewLifecycleOwner.lifecycleScope.launch {
                val image = withContext(Dispatchers.IO) { item.image?.let { EvidenceDecoder.decode(model.file(it)) } }
                b.evidence.setImageBitmap(image)
                b.noEvidence.isVisible = image == null
            }
        }
        b.capture.setText(if (item.image == null) R.string.camera else R.string.retake)
    }

    private fun pick(clock: Boolean) {
        if (!model.ready.value || childFragmentManager.findFragmentByTag("picker") != null) return
        val dialog = if (clock) EntryTimeDialog() else EntryDateDialog()
        dialog.arguments = Bundle().apply { putLong("timestamp", model.draft.occurredAt) }
        dialog.show(childFragmentManager, "picker")
    }
    private fun confirm(deletion: Boolean) {
        if (model.busy.value || model.photoPending.value || childFragmentManager.findFragmentByTag("confirmation") != null) return
        EntryConfirmation().apply { arguments = Bundle().apply { putBoolean("deletion", deletion) } }.show(childFragmentManager, "confirmation")
    }
    private fun leave() { if (!model.busy.value && !model.photoPending.value) { if (model.dirty) confirm(false) else model.discard() } }
    private fun share() {
        if (!model.ready.value) return
        val item = model.draft
        val title = item.heading.ifBlank { getString(R.string.new_title) }
        val report = getString(R.string.report, title, DateFormat.getDateTimeInstance().format(Date(item.occurredAt)), getString(if (item.resolved) R.string.solved else R.string.open), item.notes)
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject, title)).putExtra(Intent.EXTRA_TEXT, report)
        try { startActivity(Intent.createChooser(send, getString(R.string.share))) } catch (_: Exception) { message(R.string.share_error) }
    }
    private fun message(text: Int) { ui?.let { Snackbar.make(it.root, text, Snackbar.LENGTH_LONG).show() } }
    override fun onDestroyView() {
        photoTask?.cancel()
        initialized = false
        ui = null
        super.onDestroyView()
    }
}
