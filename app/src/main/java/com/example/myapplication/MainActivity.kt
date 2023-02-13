package com.example.myapplication

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log.*
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.activities.CreateNoteActivity
import com.example.myapplication.adapters.NotesAdapter
import com.example.myapplication.database.NotesDatabase
import com.example.myapplication.entities.Note
import com.example.myapplication.listeners.NotesListener


class MainActivity : AppCompatActivity(),NotesListener {

    val addNote = 1

    lateinit var notesRv: RecyclerView
    var notesAdapter: NotesAdapter = NotesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageAddNoteMain = findViewById<ImageView>(R.id.imageAddNoteMain)

        imageAddNoteMain.setOnClickListener {
            startActivityForResult(
                Intent(applicationContext, CreateNoteActivity::class.java),
                addNote
            )
        }

        notesAdapter.setListener(this)


        notesRv = findViewById(R.id.notesRecyclerView)
        notesRv.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        getNotes()


        val inputSearch = findViewById<EditText>(R.id.inputSearch)
        inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                notesAdapter.cancelTimer()
            }

            override fun afterTextChanged(s: Editable) {
                notesAdapter.searchNotes(s.toString())
            }
        })
    }

    private fun updateData(notes: List<Note>) {
        notesAdapter.notes = notes
        notesAdapter.notesSource = notes
        notesAdapter.notifyDataSetChanged()
    }

    override fun onNoteClicked(note: Note) {
        val intent = Intent(this, CreateNoteActivity::class.java)
        intent.putExtra("note_id", note.id)
        startActivity(intent)
    }




    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == addNote && resultCode == RESULT_OK -> {
                getNotes()
            }
            else -> {}
        }
    }


    private fun getNotes() {
        class GetNotesTask : AsyncTask<Void?, Void?, List<Note?>?>() {
            override fun doInBackground(vararg params: Void?): List<Note?>? {
                return NotesDatabase.getNotesDatabase(applicationContext).noteDao().getAllNotes()
            }

            override fun onPostExecute(notes: List<Note?>?) {
                super.onPostExecute(notes)
                updateData(notes?.filterNotNull() ?: listOf())
            }
        }

        GetNotesTask().execute()
    }
}
