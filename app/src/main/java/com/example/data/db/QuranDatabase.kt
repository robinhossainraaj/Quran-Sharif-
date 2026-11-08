package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        JuzEntity::class,
        BookmarkEntity::class,
        TranslationTextEntity::class,
        TranslationInfoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `translation_text` (
                        `translationId` TEXT NOT NULL,
                        `ayahId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        PRIMARY KEY(`translationId`, `ayahId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_text_ayahId` ON `translation_text` (`ayahId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_text_translationId` ON `translation_text` (`translationId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `translation_info` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `nameEn` TEXT NOT NULL,
                        `nameBn` TEXT NOT NULL,
                        `translator` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `licenseNote` TEXT NOT NULL,
                        `licenseText` TEXT NOT NULL,
                        `ayahCount` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // Populate bn_muhiuddin from existing ayah.bangla if available
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `translation_text` (`translationId`, `ayahId`, `text`)
                    SELECT 'bn_muhiuddin', `id`, `bangla` FROM `ayah`
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_sharif.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
