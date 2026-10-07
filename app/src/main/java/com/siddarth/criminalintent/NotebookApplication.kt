package com.siddarth.criminalintent

import android.app.Application
import androidx.room.Room
import com.siddarth.criminalintent.storage.IncidentStore
import java.io.File

class NotebookApplication : Application() {
    val store by lazy {
        Room.databaseBuilder(this, IncidentStore::class.java, "office-notebook.db").build()
    }

    val evidenceDirectory by lazy {
        File(filesDir, "evidence").apply { mkdirs() }
    }
}
