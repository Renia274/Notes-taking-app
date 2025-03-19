package com.example.myapplication.adapters


import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myapplication.R
import com.example.myapplication.entities.Note
import com.example.myapplication.listeners.NotesListener
import java.io.File
import java.util.*



class NotesAdapter : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    // List of notes to be displayed
    var notes: List<Note> = listOf()
    // Listener for handling note click events
    var notesListener: NotesListener? = null
    // Timer for implementing search functionality
    var timer: Timer? = null
    // List of notes as the source of truth
    var notesSource: List<Note> = listOf()

    /**

    Creates a new NoteViewHolder for displaying note items in the RecyclerView
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_container_note, parent, false)
        )
    }

    /**

    Binds a NoteViewHolder with a Note at a given position in the RecyclerView
     */
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    /**

    Sets the listener for handling note click events
     */
    fun setListener(listener: NotesListener) {
        this.notesListener = listener
    }

    /**

    Returns the number of notes in the RecyclerView
     */
    override fun getItemCount() = notes.size

    /**

    Returns the view type of a note item in the RecyclerView
     */
    override fun getItemViewType(position: Int) = NOTE_VIEW_TYPE

    /**

    ViewHolder class for displaying Note items in the RecyclerView
     */

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.textSubtitle)
        private val dateTime: TextView = itemView.findViewById(R.id.textDateTime)
        val layoutNote: LinearLayout = itemView.findViewById(R.id.layoutNote)
        private val imageNote: ImageView = itemView.findViewById(R.id.imageNote)

        /**
         * Binds a Note to this ViewHolder
         */
        fun bind(note: Note) {
            title.text = note.title
            subtitle.text = note.subtitle
            dateTime.text = note.dateTime

            // Sets up a click listener for the note item
            itemView.setOnClickListener { notesListener?.onNoteClicked(note) }

            // Hides the subtitle TextView if it's empty
            if (note.subtitle?.isEmpty() == true) {
                subtitle.visibility = View.GONE
            } else {
                subtitle.visibility = View.VISIBLE
            }

            // Sets the background color of the note item based on its color attribute
            val gradientDrawable = layoutNote.background as GradientDrawable
            if (note.color != null) {
                gradientDrawable.setColor(Color.parseColor(note.color))
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"))
            }

            // Loads the note's image using Glide and displays it if it exists
            note.imagePath?.let { imagePath ->
                try {
                    // Convert file path to URI using FileProvider
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        // Use FileProvider to create a content URI
                        val imageUri = FileProvider.getUriForFile(
                            itemView.context,
                            "${itemView.context.packageName}.imageprovider",
                            imageFile
                        )



                        // Load with Glide
                        Glide.with(itemView.context)
                            .load(imageUri)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(imageNote)

                        imageNote.visibility = View.VISIBLE
                    } else {

                        imageNote.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    imageNote.visibility = View.GONE
                }
            } ?: run {
                imageNote.visibility = View.GONE
            }
        }
    }
    /**

    Filters the notes based on a search keyword and updates the RecyclerView
     */

    fun searchNotes(searchKeyword: String) {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (searchKeyword.trim().isEmpty()) {
                    notes = notesSource
                } else {
                    val temp: ArrayList<Note> = ArrayList()
                    for (note in notesSource) {
                        val title = note.title ?: ""
                        val subtitle = note.subtitle ?: ""
                        val noteText = note.noteText ?: ""
                        if (title.contains(searchKeyword, true) ||
                            subtitle.contains(searchKeyword, true) ||
                            noteText.contains(searchKeyword, true)
                        ) {
                            temp.add(note)
                        }
                    }
                    notes = temp
                }
                Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
            }
        }, 500)
    }

    fun cancelTimer() {
        timer?.cancel()
    }

    companion object {
        const val NOTE_VIEW_TYPE = 0
    }
}