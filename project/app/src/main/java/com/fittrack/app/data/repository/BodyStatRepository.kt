package com.fittrack.app.data.repository

import com.fittrack.app.data.local.dao.BodyStatDao
import com.fittrack.app.data.local.entity.BodyStatEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyStatRepository @Inject constructor(
    private val bodyStatDao: BodyStatDao,
) {
    fun observeAll(): Flow<List<BodyStatEntity>> = bodyStatDao.observeAll()

    suspend fun add(stat: BodyStatEntity) = bodyStatDao.insert(stat)

    suspend fun delete(id: Long) = bodyStatDao.deleteById(id)
}
