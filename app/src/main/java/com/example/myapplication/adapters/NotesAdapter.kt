package com.example.myapplication.adapters


import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.entities.Note
import com.example.myapplication.listeners.NotesListener
import java.util.*


@Suppress("UNUSED_EXPRESSION")
class NotesAdapter : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    var notes: List<Note> = listOf()
    var notesListener: NotesListener? = null
    var timer: Timer? = null
    var notesSource: List<Note> = listOf()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_container_note, parent, false)
        )
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])

    }

    fun setListener(listener: NotesListener) {
        this.notesListener = listener
    }


    override fun getItemCount() = notes.size

    override fun getItemViewType(position: Int) = NOTE_VIEW_TYPE

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.textSubtitle)
        private val dateTime: TextView = itemView.findViewById(R.id.textDateTime)
        val layoutNote: LinearLayout = itemView.findViewById(R.id.layoutNote)
        private val imageNote: ImageView = itemView.findViewById(R.id.imageNote)


        fun bind(note: Note) {
            title.text = note.title
            subtitle.text = note.subtitle
            dateTime.text = note.dateTime

            itemView.setOnClickListener { notesListener?.onNoteClicked(note) }


            if (note.subtitle?.isEmpty() == true) {
                subtitle.visibility = View.GONE
            } else {
                subtitle.visibility = View.VISIBLE
            }

            val gradientDrawable = layoutNote.background as GradientDrawable
            if (note.color != null) {
                gradientDrawable.setColor(Color.parseColor(note.color))
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"))
            }

            note.imagePath?.let {
                Glide.with(itemView.context)
                    .load(it)
                    .into(imageNote)
                imageNote.visibility = View.VISIBLE
            } ?: run {
                imageNote.visibility = View.GONE
            }


        }


    }


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