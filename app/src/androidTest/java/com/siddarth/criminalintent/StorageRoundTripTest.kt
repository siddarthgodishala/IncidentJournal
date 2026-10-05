package com.siddarth.criminalintent

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.siddarth.criminalintent.storage.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageRoundTripTest {
    @Test fun savedRecordRetainsEveryFieldAfterDatabaseReopens()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<NotebookApplication>()
        val name="notebook-roundtrip.db"
        context.deleteDatabase(name)
        var store=Room.databaseBuilder(context,IncidentStore::class.java,name).build()
        try {
            val record=Incident(heading="Meeting room light",notes="Switch is stuck",occurredAt=123456789L,resolved=true,image="sample.jpg")
            store.incidents().write(record)
            store.close()
            store=Room.databaseBuilder(context,IncidentStore::class.java,name).build()
            assertEquals(record,store.incidents().read(record.key))
            store.incidents().write(record.copy(notes="Repaired",resolved=false))
            assertEquals("Repaired",store.incidents().read(record.key)!!.notes)
            store.incidents().remove(record.key)
            assertNull(store.incidents().read(record.key))
        } finally { store.close(); context.deleteDatabase(name) }
    }
}
