package com.sarmaya.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsArticleDao {
    @Query("SELECT * FROM NewsArticle ORDER BY pubDate DESC")
    fun getAllNewsFlow(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM NewsArticle ORDER BY pubDate DESC LIMIT :limit")
    suspend fun getLatestNewsSync(limit: Int = 50): List<NewsArticle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<NewsArticle>)

    @Query("DELETE FROM NewsArticle WHERE pubDate < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("DELETE FROM NewsArticle")
    suspend fun clearAll()
}
