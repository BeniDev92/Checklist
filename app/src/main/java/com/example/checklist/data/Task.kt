package com.example.checklist.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val position: Int,
    val active: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
