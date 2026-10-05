package com.siddarth.criminalintent.screens

import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors

internal fun tintNotebookToolbar(toolbar: MaterialToolbar) {
    val foreground = MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorOnSurface)
    val background = MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorSurface)
    toolbar.setBackgroundColor(background)
    toolbar.setTitleTextColor(foreground)
    toolbar.setNavigationIconTint(foreground)
    toolbar.overflowIcon = toolbar.overflowIcon?.mutate()?.also {
        DrawableCompat.setTint(it, foreground)
    }
    for (index in 0 until toolbar.menu.size()) {
        val item = toolbar.menu.getItem(index)
        item.icon = item.icon?.mutate()?.also { DrawableCompat.setTint(it, foreground) }
    }
}
