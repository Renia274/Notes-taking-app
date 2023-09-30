package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.activities.CreateNoteActivity
import com.example.myapplication.adapters.NotesAdapter
import com.example.myapplication.database.NotesDatabase
import com.example.myapplication.entities.Note
import com.example.myapplication.listeners.NotesListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), NotesListener {

    private val addNote = 1

    lateinit var notesRv: RecyclerView
    private val notesAdapter: NotesAdapter = NotesAdapter()

    // Define an ActivityResultLauncher to start CreateNoteActivity
    private lateinit var startCreateNoteActivity: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageAddNoteMain = findViewById<ImageView>(R.id.imageAddNoteMain)

        // Initialize the ActivityResultLauncher
        startCreateNoteActivity = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                getNotes()
            }
        }

        imageAddNoteMain.setOnClickListener {
            val intent = Intent(applicationContext, CreateNoteActivity::class.java)
            startCreateNoteActivity.launch(intent)
        }

        notesAdapter.setListener(this)

        // Set up the RecyclerView with the NotesAdapter and a LinearLayoutManager
        notesRv = findViewById(R.id.notesRecyclerView)
        notesRv.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // Get the initial set of notes and display them in the RecyclerView
        getNotes()

        // Set up the search
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

    // Callback function for when a note is clicked in the RecyclerView
    override fun onNoteClicked(note: Note) {
        val intent = Intent(this, CreateNoteActivity::class.java)
        intent.putExtra("note_id", note.id)
        startActivity(intent)
    }

    // Get the list of notes from the database
    private fun getNotes() {
        // Use the lifecycleScope provided by the AppCompatActivity
        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) {
                NotesDatabase.getNotesDatabase(applicationContext).noteDao().getAllNotes()
            }
            notes?.let { updateData(it.filterNotNull()) }
        }
    }
}
