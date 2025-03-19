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

    private lateinit var startCreateNoteActivity: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupActivityResultLauncher()
        setupRecyclerView()
        setupSearchFunctionality()

        // Load initial notes from database
        getNotes()
    }

    // ----------------- INITIALIZATION METHODS -----------------
    /**
     * Initializes UI views and sets up click listeners.
     */
    private fun initializeViews() {
        val imageAddNoteMain = findViewById<ImageView>(R.id.imageAddNoteMain)

        // Set up add note button click listener
        imageAddNoteMain.setOnClickListener {
            val intent = Intent(applicationContext, CreateNoteActivity::class.java)
            startCreateNoteActivity.launch(intent)
        }

        // Initialize RecyclerView
        notesRv = findViewById(R.id.notesRecyclerView)
    }

    /**
     * Sets up the ActivityResultLauncher for handling results from CreateNoteActivity.
     * Refreshes notes list when a note is created or updated.
     */
    private fun setupActivityResultLauncher() {
        startCreateNoteActivity = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                getNotes()
            }
        }
    }

    /**
     * Sets up the RecyclerView with adapter and layout manager.
     */
    private fun setupRecyclerView() {
        notesAdapter.setListener(this)

        notesRv.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    /**
     * Sets up the search functionality for filtering notes.
     */
    private fun setupSearchFunctionality() {
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

    // ----------------- DATA HANDLING METHODS -----------------
    /**
     * Updates the adapter with new note data.
     *
     * @param notes List of notes to display in the RecyclerView
     */
    private fun updateData(notes: List<Note>) {
        notesAdapter.notes = notes
        notesAdapter.notesSource = notes
        notesAdapter.notifyDataSetChanged()
    }

    /**
     * Retrieves all notes from the database using coroutines.
     * Updates the UI with the retrieved notes.
     */
    private fun getNotes() {
        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) {
                NotesDatabase.getNotesDatabase(applicationContext).noteDao().getAllNotes()
            }
            notes?.let { updateData(it.filterNotNull()) }
        }
    }

    // ----------------- LISTENER IMPLEMENTATIONS -----------------
    /**
     * Called when a note is clicked in the RecyclerView.
     * Opens the CreateNoteActivity to edit the selected note.
     *
     * @param note The Note object that was clicked
     */
    override fun onNoteClicked(note: Note) {
        val intent = Intent(this, CreateNoteActivity::class.java)
        intent.putExtra("note_id", note.id)
        startActivity(intent)
    }
}