package com.saketkhundia.pocketserver.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "server_logs")
data class ServerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val clientIp: String,
    val method: String,
    val path: String,
    val status: Int,
    val message: String
)

@Dao
interface ServerLogDao {
    @Insert suspend fun insert(e: ServerLogEntity): Long
    @Query("SELECT * FROM server_logs ORDER BY id DESC LIMIT 300") fun observe(): Flow<List<ServerLogEntity>>
    @Query("SELECT * FROM server_logs ORDER BY id DESC LIMIT 300") suspend fun snapshot(): List<ServerLogEntity>
    @Query("DELETE FROM server_logs") suspend fun clear()
    @Query("DELETE FROM server_logs WHERE id NOT IN (SELECT id FROM server_logs ORDER BY id DESC LIMIT 500)") suspend fun trim()
}

@Database(entities = [ServerLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logs(): ServerLogDao
}
