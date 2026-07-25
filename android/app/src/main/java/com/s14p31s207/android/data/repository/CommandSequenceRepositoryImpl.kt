package com.s14p31s207.android.data.repository

import com.s14p31s207.android.data.db.CommandSequenceDao
import com.s14p31s207.android.domain.repository.CommandSequenceRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CommandSequenceRepositoryImpl @Inject constructor(
    private val commandSequenceDao: CommandSequenceDao
) : CommandSequenceRepository {

    override suspend fun nextNumber(): Long {
        return commandSequenceDao.incrementAndGet(currentDateKey())
    }

    override suspend fun nextId(): String {
        val dateKey = currentDateKey()
        val nextNumber = commandSequenceDao.incrementAndGet(dateKey)
        return "req-$dateKey-$nextNumber"
    }

    private fun currentDateKey(): String {
        return LocalDate.now().format(DATE_KEY_FORMATTER)
    }

    private companion object {
        val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd")
    }
}
