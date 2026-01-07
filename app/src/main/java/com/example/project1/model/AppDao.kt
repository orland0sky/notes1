package com.example.project1.data

import androidx.room.*
import com.example.project1.model.Note
import com.example.project1.model.User

@Dao
interface AppDao {
    // Logic Login & Register
    @Insert
    suspend fun registerUser(user: User)

    @Query("SELECT * FROM users WHERE username = :user AND password = :pass LIMIT 1")
    suspend fun loginUser(user: String, pass: String): User?

    // Logic CRUD Notes
    @Insert
    suspend fun insertNote(note: Note)

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<Note>

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}