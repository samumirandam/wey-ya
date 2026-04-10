package com.weyya.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.weyya.app.data.db.entity.WhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: WhitelistEntity)

    @Delete
    suspend fun delete(entry: WhitelistEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE phoneNumber = :number)")
    suspend fun isWhitelisted(number: String): Boolean

    @Query("SELECT * FROM whitelist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WhitelistEntity>>

    @Query("DELETE FROM whitelist WHERE phoneNumber = :number")
    suspend fun deleteByNumber(number: String)
}
