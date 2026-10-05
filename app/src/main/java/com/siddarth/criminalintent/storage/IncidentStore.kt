package com.siddarth.criminalintent.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "notebook")
data class Incident(
    @PrimaryKey val key: String = UUID.randomUUID().toString(),
    val heading: String = "",
    val notes: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    val image: String? = null
)

@Dao
interface IncidentQueries {
    @Query("SELECT * FROM notebook ORDER BY occurredAt DESC, key ASC")
    fun watch(): Flow<List<Incident>>
    @Query("SELECT * FROM notebook WHERE `key` = :key")
    suspend fun read(key: String): Incident?
    @Upsert suspend fun write(incident: Incident)
    @Query("DELETE FROM notebook WHERE `key` = :key")
    suspend fun remove(key: String)
}

@Database(entities = [Incident::class], version = 1, exportSchema = true)
abstract class IncidentStore : RoomDatabase() {
    abstract fun incidents(): IncidentQueries
}
