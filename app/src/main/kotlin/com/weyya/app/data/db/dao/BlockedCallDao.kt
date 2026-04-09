package com.weyya.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.weyya.app.data.db.entity.BlockedCallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    @Insert
    suspend fun insert(call: BlockedCallEntity)

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BlockedCallEntity>>

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE timestamp >= :since")
    fun getBlockedCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_calls")
    fun getTotalBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE wasEventuallyAllowed = 1")
    fun getBypassCount(): Flow<Int>

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC LIMIT 1")
    fun getLastBlocked(): Flow<BlockedCallEntity?>

    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()
}
