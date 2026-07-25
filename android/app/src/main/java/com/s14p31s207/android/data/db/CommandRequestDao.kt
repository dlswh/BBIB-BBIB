package com.s14p31s207.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface CommandRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(commandRequest: CommandRequestEntity)
}
