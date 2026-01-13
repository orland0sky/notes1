package com.example.project1.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
) : Serializable