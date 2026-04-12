package com.sarmaya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "NewsArticle")
data class NewsArticle(
    @PrimaryKey
    val id: String, // Typically the link URL
    val title: String,
    val link: String,
    val description: String,
    val pubDate: Long,
    val source: String,
    val category: String,
    val relatedSector: String? = null,
    val cachedAt: Long
)
