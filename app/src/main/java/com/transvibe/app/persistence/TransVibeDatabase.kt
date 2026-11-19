package com.transvibe.app.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        JobEntity::class,
        TranscriptSegmentEntity::class,
        TranslationEntity::class,
        CueEditEntity::class,
        BenchmarkRunEntity::class,
        FreezeRecordEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class TransVibeDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun benchmarkDao(): BenchmarkDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `freeze_records` (
                        `modelId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `profileVersion` INTEGER NOT NULL,
                        `approvedByOwner` INTEGER NOT NULL,
                        `ownerSignoffTimestampMs` INTEGER,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`modelId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `translations` ADD COLUMN `sourceLanguage` TEXT NOT NULL DEFAULT 'en'")
                db.execSQL("ALTER TABLE `translations` ADD COLUMN `englishIntermediateText` TEXT")
                db.execSQL("ALTER TABLE `translations` ADD COLUMN `stage1Status` TEXT NOT NULL DEFAULT 'NOT_REQUIRED'")
                db.execSQL("ALTER TABLE `translations` ADD COLUMN `stage2Status` TEXT NOT NULL DEFAULT 'COMPLETED'")
            }
        }

        @Volatile
        private var INSTANCE: TransVibeDatabase? = null

        fun getInstance(context: Context): TransVibeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TransVibeDatabase::class.java,
                    "transvibe.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
