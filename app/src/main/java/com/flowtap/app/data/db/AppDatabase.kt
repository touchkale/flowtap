package com.flowtap.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ModeEntity::class, TagEntity::class, SessionEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modeDao(): ModeDao
    abstract fun tagDao(): TagDao
    abstract fun sessionDao(): SessionDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE modes ADD COLUMN keepAwake INTEGER")
                db.execSQL("ALTER TABLE modes ADD COLUMN autoEndMinutes INTEGER")
                db.execSQL("ALTER TABLE modes ADD COLUMN allowlistOnly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE modes ADD COLUMN allowedAppsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE modes ADD COLUMN launchPackage TEXT")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE modes ADD COLUMN dailyGoalMinutes INTEGER NOT NULL DEFAULT 300")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "flowtap.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
