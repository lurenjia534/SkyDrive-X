package com.lurenjia534.skydrivex.data.repository

import com.lurenjia534.skydrivex.auth.GraphTokenProvider
import com.lurenjia534.skydrivex.data.local.index.IndexDao
import com.lurenjia534.skydrivex.data.local.index.IndexItemEntity
import com.lurenjia534.skydrivex.data.local.index.IndexPreferenceRepository
import com.lurenjia534.skydrivex.data.model.driveitem.DriveDeltaResponse
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
import com.lurenjia534.skydrivex.data.remote.GraphApiService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class IndexRepository @Inject constructor(
    private val indexDao: IndexDao,
    private val prefs: IndexPreferenceRepository,
    private val api: GraphApiService,
    private val tokenProvider: GraphTokenProvider
) {

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        indexDao.clearAll()
        prefs.resetForRebuild()
    }

    suspend fun sync(rebuild: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (rebuild) {
                indexDao.clearAll()
                prefs.resetForRebuild()
            }

            val bearer = "Bearer ${tokenProvider.getAccessToken()}"
            val snapshot = prefs.snapshot.first()

            var response: DriveDeltaResponse = if (snapshot.deltaLink.isNullOrBlank() || rebuild) {
                api.getRootDelta(token = bearer)
            } else {
                api.followDeltaLink(snapshot.deltaLink, bearer)
            }

            var nextLink = response.nextLink
            var latestDelta: String? = response.deltaLink

            applyPage(response)

            while (!nextLink.isNullOrBlank()) {
                response = api.followDeltaLink(nextLink!!, bearer)
                nextLink = response.nextLink
                latestDelta = response.deltaLink ?: latestDelta
                applyPage(response)
            }

            if (!latestDelta.isNullOrBlank()) {
                prefs.updateDeltaLink(latestDelta)
            }
            prefs.updateLastSync(System.currentTimeMillis())
        }
    }

    private suspend fun applyPage(response: DriveDeltaResponse) {
        val deletions = response.value
            .filter { it.deleted != null }
            .mapNotNull { it.id }
        if (deletions.isNotEmpty()) {
            indexDao.deleteByIds(deletions)
        }

        val upserts = response.value
            .filter { it.deleted == null }
            .mapNotNull { it.toEntity() }
        if (upserts.isNotEmpty()) {
            indexDao.upsertAll(upserts)
        }
    }
}

private fun DriveItemDto.toEntity(): IndexItemEntity? {
    val id = id ?: return null
    val itemName = name?.takeIf { it.isNotBlank() } ?: return null
    val normalizedName = itemName.lowercase()
    val isFolder = folder != null
    val ext = when {
        isFolder -> null
        else -> normalizedName.substringAfterLast('.', "").takeIf { it.isNotBlank() }
    }

    val driveId = parentReference?.driveId ?: ""
    val parentId = parentReference?.id
    val pathLower = parentReference?.path
        ?.substringAfter(":/", missingDelimiterValue = "")
        ?.lowercase()
        ?.ifBlank { null }

    val lastModifiedMillis = lastModifiedDateTime
        ?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }

    return IndexItemEntity(
        itemId = id,
        driveId = driveId,
        parentId = parentId,
        nameLower = normalizedName,
        ext = ext,
        isFolder = isFolder,
        size = size,
        lastModified = lastModifiedMillis,
        pathLower = pathLower,
        lastIndexedAt = System.currentTimeMillis()
    )
}

