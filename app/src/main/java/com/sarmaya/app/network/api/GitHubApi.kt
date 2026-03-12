package com.sarmaya.app.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

/**
 * GitHub REST API for checking latest release.
 * Base URL: https://api.github.com
 */
interface GitHubApi {

    @GET("/repos/dev-Aatif/sarmaya/releases/latest")
    suspend fun getLatestRelease(): GitHubReleaseResponse
}

@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    @Json(name = "tag_name") val tagName: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "body") val body: String?,
    @Json(name = "html_url") val htmlUrl: String?,
    @Json(name = "published_at") val publishedAt: String?,
    @Json(name = "assets") val assets: List<GitHubAsset>?
)

@JsonClass(generateAdapter = true)
data class GitHubAsset(
    @Json(name = "name") val name: String?,
    @Json(name = "browser_download_url") val browserDownloadUrl: String?,
    @Json(name = "content_type") val contentType: String?
)
