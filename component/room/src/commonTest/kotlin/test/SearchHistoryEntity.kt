package io.github.kamo030.koinboot.component.room.test

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val content: String,
    val timestamp: String,
)