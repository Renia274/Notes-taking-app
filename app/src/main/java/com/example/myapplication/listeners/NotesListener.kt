package com.example.myapplication.listeners

import com.example.myapplication.entities.Note

interface NotesListener {
    fun onNoteClicked(note: Note)
}