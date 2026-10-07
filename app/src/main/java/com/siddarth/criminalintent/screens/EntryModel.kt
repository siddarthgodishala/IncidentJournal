package com.siddarth.criminalintent.screens

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.siddarth.criminalintent.NotebookApplication
import com.siddarth.criminalintent.storage.Incident
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntryModel(
    application: Application,
    private val state: SavedStateHandle
) : AndroidViewModel(application) {
    private val notebook = application as NotebookApplication
    private val dao = notebook.store.incidents()
    private val loaded = CompletableDeferred<Unit>()

    val ready = MutableStateFlow(false)
    val problem = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val closed = MutableStateFlow(false)
    val revision = MutableStateFlow(0)
    val photoPending = MutableStateFlow(state.get<String>(CAMERA_FILE) != null)

    val existing: Boolean
        get() = state.get<String>("entry_key") != null

    var dirty: Boolean
        get() = state["dirty"] ?: false
        private set(value) {
            state["dirty"] = value
        }

    // The editable draft lives in saved state, not Room, until Save is pressed.
    var draft: Incident
        get() {
            val bundle = checkNotNull(state.get<Bundle>(DRAFT))
            return Incident(
                key = checkNotNull(bundle.getString("key")),
                heading = bundle.getString("heading").orEmpty(),
                notes = bundle.getString("notes").orEmpty(),
                occurredAt = bundle.getLong("time"),
                resolved = bundle.getBoolean("resolved"),
                image = bundle.getString("image")
            )
        }
        private set(value) {
            state[DRAFT] = Bundle().apply {
                putString("key", value.key)
                putString("heading", value.heading)
                putString("notes", value.notes)
                putLong("time", value.occurredAt)
                putBoolean("resolved", value.resolved)
                putString("image", value.image)
            }
            revision.value += 1
        }

    // Keep the saved attachment while a replacement is only part of an unsaved draft.
    private var originalImage: String?
        get() = state["original_image"]
        set(value) { state["original_image"] = value }

    // Remember the camera destination so its result can be handled after recreation.
    private var pendingImage: String?
        get() = state[CAMERA_FILE]
        set(value) {
            state[CAMERA_FILE] = value
            photoPending.value = value != null
        }

    init {
        viewModelScope.launch {
            try {
                if (!state.contains(DRAFT)) {
                    val record = if (existing) {
                        checkNotNull(dao.read(checkNotNull(state["entry_key"])))
                    } else {
                        Incident()
                    }
                    draft = record
                    originalImage = record.image
                }
                ready.value = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                problem.value = true
            } finally {
                loaded.complete(Unit)
            }
        }
    }

    fun change(edit: (Incident) -> Incident) {
        if (!ready.value || busy.value || closed.value) return
        val updated = edit(draft)
        if (updated != draft) {
            draft = updated
            dirty = true
        }
    }

    fun file(name: String): File = File(notebook.evidenceDirectory, name)

    suspend fun beginPhoto(): File? {
        if (!ready.value || busy.value || photoPending.value || closed.value) return null
        busy.value = true
        val name = "${UUID.randomUUID()}.jpg"
        return try {
            val destination = withContext(Dispatchers.IO) {
                file(name).also { check(it.createNewFile()) }
            }
            pendingImage = name
            destination
        } catch (error: Exception) {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { file(name).delete() }
            throw error
        } finally {
            busy.value = false
        }
    }

    fun receivePhoto(success: Boolean, onInvalidPhoto: () -> Unit = {}) {
        viewModelScope.launch {
            loaded.await()
            val name = pendingImage ?: return@launch
            busy.value = true
            try {
                val valid = success && withContext(Dispatchers.IO) {
                    EvidenceDecoder.decode(file(name))?.let { bitmap ->
                        bitmap.recycle()
                        true
                    } ?: false
                }
                if (valid && ready.value && !closed.value) {
                    val previousDraftImage = draft.image
                    draft = draft.copy(image = name)
                    dirty = true
                    // Only discarded draft photos can be deleted before the record is saved.
                    if (previousDraftImage != originalImage) {
                        deleteFiles(previousDraftImage)
                    }
                } else {
                    deleteFiles(name)
                    if (success) onInvalidPhoto()
                }
            } finally {
                pendingImage = null
                busy.value = false
            }
        }
    }

    fun save(onError: () -> Unit) {
        if (!canFinish() || draft.heading.isBlank()) return
        busy.value = true
        viewModelScope.launch {
            try {
                val record = draft.copy(
                    heading = draft.heading.trim(),
                    notes = draft.notes.trim()
                )
                // Commit the new attachment reference before removing the previous saved file.
                dao.write(record)
                if (originalImage != record.image) deleteFiles(originalImage)
                originalImage = record.image
                dirty = false
                closed.value = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onError()
            } finally {
                busy.value = false
            }
        }
    }

    fun remove(onError: () -> Unit) {
        if (!canFinish()) return
        busy.value = true
        viewModelScope.launch {
            try {
                dao.remove(draft.key)
                deleteFiles(originalImage, draft.image)
                closed.value = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onError()
            } finally {
                busy.value = false
            }
        }
    }

    fun discard() {
        if (busy.value || photoPending.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                if (ready.value && draft.image != originalImage) deleteFiles(draft.image)
                closed.value = true
            } finally {
                busy.value = false
            }
        }
    }

    private fun canFinish() = ready.value && !busy.value && !photoPending.value && !closed.value

    private suspend fun deleteFiles(vararg names: String?) {
        withContext(Dispatchers.IO) {
            names.filterNotNull().distinct().forEach { file(it).delete() }
        }
    }

    private companion object {
        const val DRAFT = "draft"
        const val CAMERA_FILE = "camera_pending"
    }
}
