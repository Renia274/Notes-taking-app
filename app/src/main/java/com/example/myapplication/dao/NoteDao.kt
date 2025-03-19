package com.example.myapplication.dao

import androidx.room.*
import com.example.myapplication.entities.Note

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): List<Note?>?

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteById(noteId: Int): Note?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(note: Note)

    @Delete
    fun deleteNote(note: Note?)
}