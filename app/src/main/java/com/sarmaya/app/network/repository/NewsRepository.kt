package com.sarmaya.app.network.repository

import com.sarmaya.app.data.NewsArticle
import com.sarmaya.app.data.NewsArticleDao
import com.sarmaya.app.network.ConnectivityChecker
import com.sarmaya.app.network.rss.GoogleRssParser
import kotlinx.coroutines.flow.Flow

class NewsRepository(
    private val newsDao: NewsArticleDao,
    private val rssParser: GoogleRssParser,
    private val connectivityChecker: ConnectivityChecker
) {
    fun getAllNewsFlow(): Flow<List<NewsArticle>> {
        return newsDao.getAllNewsFlow()
    }

    suspend fun refreshNews(): Result<Unit> {
        if (!connectivityChecker.isOnline()) return Result.failure(Exception("No internet connection"))
        return try {
            val articles = rssParser.fetchAndParse()
            if (articles.isNotEmpty()) {
                newsDao.insertAll(articles)
                
                // Keep only fresh articles (last 7 days)
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                newsDao.deleteOlderThan(sevenDaysAgo)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
