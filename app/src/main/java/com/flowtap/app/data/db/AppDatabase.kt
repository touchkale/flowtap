package com.flowtap.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ModeEntity::class, TagEntity::class, SessionEntity::class],
    version = 4,
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

        /**
         * v4: dndEnabled (boolean) becomes notificationMode (none/silent/dnd).
         * Prior DND-on modes stay DND; everything else becomes silent (the new
         * default). Table is rebuilt to drop the dead column.
         */
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE modes_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, iconName TEXT NOT NULL, accentIndex INTEGER NOT NULL, " +
                        "notificationMode TEXT NOT NULL DEFAULT 'silent', strictMode INTEGER NOT NULL, " +
                        "blockedAppsJson TEXT NOT NULL, keepAwake INTEGER, autoEndMinutes INTEGER, " +
                        "allowlistOnly INTEGER NOT NULL DEFAULT 0, " +
                        "allowedAppsJson TEXT NOT NULL DEFAULT '[]', launchPackage TEXT, " +
                        "dailyGoalMinutes INTEGER NOT NULL DEFAULT 120, createdAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO modes_new (id, name, iconName, accentIndex, notificationMode, " +
                        "strictMode, blockedAppsJson, keepAwake, autoEndMinutes, allowlistOnly, " +
                        "allowedAppsJson, launchPackage, dailyGoalMinutes, createdAt) " +
                        "SELECT id, name, iconName, accentIndex, " +
                        "CASE WHEN dndEnabled THEN 'dnd' ELSE 'silent' END, " +
                        "strictMode, blockedAppsJson, keepAwake, autoEndMinutes, allowlistOnly, " +
                        "allowedAppsJson, launchPackage, dailyGoalMinutes, createdAt FROM modes"
                )
                db.execSQL("DROP TABLE modes")
                db.execSQL("ALTER TABLE modes_new RENAME TO modes")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "flowtap.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
