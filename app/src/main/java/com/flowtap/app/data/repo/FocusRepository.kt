package com.flowtap.app.data.repo

import com.flowtap.app.data.db.ModeDao
import com.flowtap.app.data.db.SessionDao
import com.flowtap.app.data.db.TagDao
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.FocusTag
import com.flowtap.app.data.model.SessionRecord
import com.flowtap.app.data.model.toDomain
import com.flowtap.app.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FocusRepository(
    private val modeDao: ModeDao,
    private val tagDao: TagDao,
    private val sessionDao: SessionDao,
) {
    val modes: Flow<List<FocusMode>> = modeDao.observeAll().map { list -> list.map { it.toDomain() } }
    val tags: Flow<List<FocusTag>> = tagDao.observeAll().map { list -> list.map { it.toDomain() } }
    val sessions: Flow<List<SessionRecord>> = sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Inserts the default "Deep Work" mode on first launch. */
    suspend fun ensureSeeded() {
        if (modeDao.count() > 0) return
        modeDao.insert(
            FocusMode(
                name = "Deep Work",
                iconName = "work",
                accentIndex = 0,
                notificationMode = com.flowtap.app.data.model.NotificationMode.SILENT,
                strictMode = false,
                blockedApps = listOf(
                    "com.instagram.android",
                    "com.google.android.youtube",
                    "com.reddit.frontpage",
                    "com.android.chrome",
                ),
                dailyGoalMinutes = 120,
            ).toEntity()
        )
    }

    suspend fun getMode(id: Long): FocusMode? = modeDao.getById(id)?.toDomain()

    suspend fun getTagByUid(uidHex: String): FocusTag? = tagDao.getByUid(uidHex)?.toDomain()

    suspend fun addMode(mode: FocusMode): Long = modeDao.insert(mode.toEntity())

    suspend fun updateMode(mode: FocusMode) = modeDao.update(mode.toEntity())

    /** Deletes the mode and (F5) all tags bound to it. */
    suspend fun deleteMode(mode: FocusMode) {
        modeDao.delete(mode.toEntity())
        tagDao.deleteByMode(mode.id)
    }

    suspend fun addTag(tag: FocusTag): Long = tagDao.insert(tag.toEntity())

    suspend fun updateTag(tag: FocusTag) = tagDao.update(tag.toEntity())

    suspend fun deleteTag(tag: FocusTag) = tagDao.delete(tag.toEntity())

    suspend fun touchTag(id: Long, lastUsedAt: Long = System.currentTimeMillis()) {
        tagDao.getById(id)?.let { tagDao.update(it.copy(lastUsedAt = lastUsedAt)) }
    }

    suspend fun insertSession(session: SessionRecord): Long = sessionDao.insert(session.toEntity())

    suspend fun updateSession(session: SessionRecord) = sessionDao.update(session.toEntity())

    suspend fun deleteSession(id: Long) = sessionDao.deleteById(id)

    suspend fun getInProgressSession(): SessionRecord? = sessionDao.getInProgress()?.toDomain()

    suspend fun getSession(id: Long): SessionRecord? = sessionDao.getById(id)?.toDomain()
}
