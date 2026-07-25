package com.s14p31s207.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class CommandSequenceDao {

    @Query("SELECT * FROM command_sequences WHERE prefix = :prefix LIMIT 1")
    abstract suspend fun getByPrefix(prefix: String): CommandSequence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(sequence: CommandSequence)

    @Query("UPDATE command_sequences SET last_number = :lastNumber WHERE prefix = :prefix")
    abstract suspend fun updateLastNumber(prefix: String, lastNumber: Long)

    @Transaction
    open suspend fun incrementAndGet(prefix: String): Long {
        val nextNumber = (getByPrefix(prefix)?.lastNumber ?: 0L) + 1L
        upsert(CommandSequence(prefix = prefix, lastNumber = nextNumber))
        return nextNumber
    }
}
