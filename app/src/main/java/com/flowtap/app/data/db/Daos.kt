package com.flowtap.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {
    @Query("SELECT * FROM modes ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ModeEntity>>

    @Query("SELECT * FROM modes WHERE id = :id")
    fun observeById(id: Long): Flow<ModeEntity?>

    @Query("SELECT * FROM modes WHERE id = :id")
    suspend fun getById(id: Long): ModeEntity?

    @Query("SELECT COUNT(*) FROM modes")
    suspend fun count(): Int

    @Insert
    suspend fun insert(mode: ModeEntity): Long

    @Update
    suspend fun update(mode: ModeEntity)

    @Delete
    suspend fun delete(mode: ModeEntity)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE uidHex = :uidHex LIMIT 1")
    suspend fun getByUid(uidHex: String): TagEntity?

    @Query("SELECT * FROM tags WHERE modeId = :modeId")
    fun observeByMode(modeId: Long): Flow<List<TagEntity>>

    @Query("DELETE FROM tags WHERE modeId = :modeId")
    suspend fun deleteByMode(modeId: Long)

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE endedAt = 0 LIMIT 1")
    suspend fun getInProgress(): SessionEntity?

    @Query("SELECT COALESCE(MAX(startedAt), 0) FROM sessions")
    suspend fun lastStartedAt(): Long

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
