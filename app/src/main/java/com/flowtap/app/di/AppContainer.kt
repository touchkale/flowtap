package com.flowtap.app.di

import android.content.Context
import com.flowtap.app.data.db.AppDatabase
import com.flowtap.app.data.prefs.SettingsDataStore
import com.flowtap.app.data.repo.FocusRepository
import com.flowtap.app.data.repo.StatsRepository

/**
 * Manual dependency container (no DI framework, by design — easier to reason about).
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(appContext) }

    val focusRepository: FocusRepository by lazy {
        FocusRepository(database.modeDao(), database.tagDao(), database.sessionDao())
    }

    val statsRepository: StatsRepository by lazy { StatsRepository(focusRepository) }
}
