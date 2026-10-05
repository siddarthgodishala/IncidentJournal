package com.siddarth.criminalintent

import android.os.Bundle
import android.graphics.Color
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.siddarth.criminalintent.screens.NotebookFragment
import com.siddarth.criminalintent.screens.EntryFragment

class NotebookActivity : AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge(SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT), SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContentView(R.layout.activity_notebook)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pages)) { view, insets ->
            val space = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(space.left, space.top, space.right, space.bottom)
            insets
        }
        if (state == null) supportFragmentManager.beginTransaction().replace(R.id.pages, NotebookFragment()).commit()
    }

    fun edit(key: String? = null) {
        supportFragmentManager.beginTransaction().setReorderingAllowed(true)
            .replace(R.id.pages, EntryFragment().apply { arguments = Bundle().apply { putString("entry_key", key) } })
            .addToBackStack("entry").commit()
    }
}
