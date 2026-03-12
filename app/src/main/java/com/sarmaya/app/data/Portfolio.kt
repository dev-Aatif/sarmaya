package com.sarmaya.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-created portfolio. Users can have multiple portfolios.
 * The default portfolio (id=1) is created automatically on first launch.
 */
@Entity(tableName = "Portfolio")
data class Portfolio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)
