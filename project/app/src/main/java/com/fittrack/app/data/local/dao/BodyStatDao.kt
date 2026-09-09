package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fittrack.app.data.local.entity.BodyStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyStatDao {

    @Query("SELECT * FROM body_stats ORDER BY recordedAt ASC")
    fun observeAll(): Flow<List<BodyStatEntity>>

    @Insert
    suspend fun insert(stat: BodyStatEntity): Long

    @Query("DELETE FROM body_stats WHERE id = :id")
    suspend fun deleteById(id: Long)
}
