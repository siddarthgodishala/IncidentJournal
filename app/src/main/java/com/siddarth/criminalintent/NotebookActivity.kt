package com.siddarth.criminalintent

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotebookActivity : AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notebook)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pages)) { view, insets ->
            val space = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(space.left, space.top, space.right, space.bottom)
            insets
        }
    }
}
