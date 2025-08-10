package com.lurenjia534.skydrivex.data.repository

import com.lurenjia534.skydrivex.data.model.RecycleBinItemDto
import com.lurenjia534.skydrivex.data.remote.GraphApiService
import com.lurenjia534.skydrivex.data.remote.SharePointApiService
import javax.inject.Inject
import javax.inject.Singleton
import java.net.URL

@Singleton
class RecycleBinRepository @Inject constructor(
    private val graphApiService: GraphApiService,
    private val sharePointApiService: SharePointApiService
) {
    private suspend fun getSiteUrl(token: String): String? {
        val drive = graphApiService.getDrive(token)
        val webUrl = drive.webUrl ?: return null
        // The webUrl is for the "Documents" library, e.g., "https://tenant-my.sharepoint.com/personal/user_site/Documents"
        // The REST API root is at the site level, e.g., "https://tenant-my.sharepoint.com/personal/user_site"
        return URL(webUrl).let { "${it.protocol}://${it.host}${it.path.substringBeforeLast("/")}" }
    }

    suspend fun getRecycleBinItems(token: String): List<RecycleBinItemDto> {
        val siteUrl = getSiteUrl(token) ?: return emptyList()
        val recycleBinUrl = "$siteUrl/_api/web/recyclebin"
        return sharePointApiService.getRecycleBinItems(recycleBinUrl, token).d.results
    }

    suspend fun restoreItem(token: String, itemId: String) {
        val siteUrl = getSiteUrl(token) ?: return
        val restoreUrl = "$siteUrl/_api/web/recyclebin('$itemId')/restore()"
        sharePointApiService.restoreRecycleBinItem(restoreUrl, token)
    }

    suspend fun deleteItem(token: String, itemId: String) {
        val siteUrl = getSiteUrl(token) ?: return
        val deleteUrl = "$siteUrl/_api/web/recyclebin('$itemId')/deleteObject()"
        sharePointApiService.deleteRecycleBinItem(deleteUrl, token)
    }
}
