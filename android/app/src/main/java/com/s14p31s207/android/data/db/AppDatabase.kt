package com.s14p31s207.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        IpcDevice::class,
        CommandSequence::class,
        CommandRequestEntity::class,
        LogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ipcDeviceDao(): IpcDeviceDao
    abstract fun commandSequenceDao(): CommandSequenceDao
    abstract fun commandRequestDao(): CommandRequestDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `command_sequences` (
                        `prefix` TEXT NOT NULL,
                        `last_number` INTEGER NOT NULL,
                        PRIMARY KEY(`prefix`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `command_requests` (
                        `id` TEXT NOT NULL,
                        `cmd` TEXT NOT NULL,
                        `timestamp` TEXT NOT NULL,
                        `data` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `logs` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `timestamp` TEXT NOT NULL,
                        `cmd` TEXT,
                        `status` TEXT,
                        `message` TEXT,
                        `requestId` TEXT,
                        `errorCode` INTEGER,
                        `errorName` TEXT,
                        `category` TEXT,
                        `severity` TEXT,
                        `context` TEXT,
                        `solution` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        // wifi_profiles 제거, ipc_devices 추가
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `wifi_profiles`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ipc_devices` (
                        `name` TEXT NOT NULL,
                        `ip` TEXT,
                        `port` TEXT,
                        `macAddress` TEXT,
                        PRIMARY KEY(`name`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wifi_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `logs` ADD COLUMN `ipcId` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}