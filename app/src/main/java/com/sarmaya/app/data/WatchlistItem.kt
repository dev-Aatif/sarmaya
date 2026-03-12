package com.sarmaya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "WatchlistItem")
data class WatchlistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stockSymbol: String,
    val addedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
