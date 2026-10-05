package com.siddarth.criminalintent.screens

import android.app.Dialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.siddarth.criminalintent.R
import java.util.Calendar

class EntryDateDialog : DialogFragment() {
    override fun onCreateDialog(state: Bundle?): Dialog {
        val date = Calendar.getInstance().apply {
            timeInMillis = requireArguments().getLong("timestamp")
        }
        return DatePickerDialog(requireContext(), { _, year, month, day ->
            date.set(year, month, day)
            parentFragmentManager.setFragmentResult("timestamp", Bundle().apply {
                putLong("value", date.timeInMillis)
            })
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
    }
}

class EntryTimeDialog : DialogFragment() {
    override fun onCreateDialog(state: Bundle?): Dialog {
        val date = Calendar.getInstance().apply {
            timeInMillis = requireArguments().getLong("timestamp")
        }
        return TimePickerDialog(requireContext(), { _, hour, minute ->
            date.set(Calendar.HOUR_OF_DAY, hour)
            date.set(Calendar.MINUTE, minute)
            date.set(Calendar.SECOND, 0)
            date.set(Calendar.MILLISECOND, 0)
            parentFragmentManager.setFragmentResult("timestamp", Bundle().apply {
                putLong("value", date.timeInMillis)
            })
        }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE),
            android.text.format.DateFormat.is24HourFormat(requireContext()))
    }
}

class EntryConfirmation : DialogFragment() {
    override fun onCreateDialog(state: Bundle?): Dialog {
        val deletion = requireArguments().getBoolean("deletion")
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (deletion) R.string.delete_title else R.string.discard_title)
            .setMessage(if (deletion) R.string.delete_body else R.string.discard_body)
            .setNegativeButton(if (deletion) R.string.cancel else R.string.keep_editing, null)
            .setPositiveButton(if (deletion) R.string.delete else R.string.discard) { _, _ ->
                parentFragmentManager.setFragmentResult(if (deletion) "remove" else "discard", Bundle())
            }.create()
    }
}
