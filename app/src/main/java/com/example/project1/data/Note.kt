package com.example.project1.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isChecked: Boolean = false,
    val color: Int = -1,
    val label: String? = null,
    val type: String = "text",
    val reminderTime: Long? = null // Kolom baru untuk waktu pengingat
) : Serializable