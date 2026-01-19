package com.example.project1.data

import androidx.room.*
import com.example.project1.model.Note
import com.example.project1.model.User
import com.example.project1.model.Label

@Dao
interface AppDao {
    // --- LOGIKA AUTH (USER) ---
    @Insert
    suspend fun registerUser(user: User)

    @Query("SELECT * FROM users WHERE username = :user AND password = :pass LIMIT 1")
    suspend fun loginUser(user: String, pass: String): User?

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    // --- LOGIKA CATATAN (NOTES) ---
    @Insert
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNotePermanent(note: Note)

    // Dashboard: Hanya yang TIDAK punya label DAN TIDAK punya pengingat (Ruang Tamu Umum)
    @Query("SELECT * FROM notes WHERE label IS NULL AND reminderTime IS NULL AND isArchived = 0 AND isDeleted = 0 ORDER BY id DESC")
    suspend fun getDashboardNotes(): List<Note>

    // Halaman Label: Hanya yang punya label spesifik
    @Query("SELECT * FROM notes WHERE label = :labelName AND isArchived = 0 AND isDeleted = 0 ORDER BY id DESC")
    suspend fun getNotesByLabel(labelName: String): List<Note>

    // Halaman Arsip: Semua yang diarsipkan
    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY id DESC")
    suspend fun getArchivedNotes(): List<Note>

    // Halaman Sampah: Semua yang dihapus
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY id DESC")
    suspend fun getDeletedNotes(): List<Note>

    // Halaman Pengingat: Hanya yang punya jadwal pengingat DAN belum diarsipkan/dihapus
    @Query("SELECT * FROM notes WHERE reminderTime IS NOT NULL AND isArchived = 0 AND isDeleted = 0 ORDER BY reminderTime ASC")
    suspend fun getReminderNotes(): List<Note>

    // --- LOGIKA LABEL ---
    @Insert
    suspend fun insertLabel(label: Label)

    @Query("SELECT * FROM labels ORDER BY name ASC")
    suspend fun getAllLabels(): List<Label>

    @Delete
    suspend fun deleteLabel(label: Label)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): Note?

}