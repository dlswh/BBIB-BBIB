package com.s14p31s207.android.domain.repository

interface CommandSequenceRepository {
    suspend fun nextNumber(): Long
    suspend fun nextId(): String
}
