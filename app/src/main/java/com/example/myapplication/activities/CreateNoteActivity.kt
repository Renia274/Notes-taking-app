package com.example.myapplication.activities


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.database.NotesDatabase
import com.example.myapplication.entities.Note
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class CreateNoteActivity : AppCompatActivity() {

    private var inputNoteTitle: EditText? = null
    private var REQUEST_CODE_TAKE_IMAGE = 3
    private var inputNoteSubtitle: EditText? = null
    private var inputNoteText: EditText? = null
    private var textDateTime: TextView? = null
    private var viewSubtitleIndicator: View? = null
    private var imageNote: ImageView? = null
    private var textWebURL: TextView? = null
    private var layoutWebURL: LinearLayout? = null
    private var selectedNoteColor: String? = null
    private val REQUEST_CODE_STORAGE_PERMISSION = 1
    private val REQUEST_CODE_SELECT_IMAGE = 2
    private var dialogAddURL: AlertDialog? = null
    private var dialogDeleteNote: AlertDialog? = null
    private var alreadyAvailableNote: Note? = null
    private var selectedImagePath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        // Get references to UI elements
        val imageBack = findViewById<ImageView>(com.example.myapplication.R.id.imageBack)
        val imageSave = findViewById<ImageView>(com.example.myapplication.R.id.imageSave)


        inputNoteTitle = findViewById(R.id.inputNoteTitle)
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle)
        inputNoteText = findViewById(R.id.inputNoteText)
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator)
        imageNote = findViewById(R.id.imageNote)
        textWebURL = findViewById(R.id.textWebURL)
        layoutWebURL = findViewById(R.id.layoutWebURL)
        textDateTime = findViewById(R.id.textDateTime)
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator)




        textDateTime?.text = SimpleDateFormat(
            "EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()
        ).format(Date().time)

        // Set onClickListener for imageBack and imageSave ImageView
        imageBack.setOnClickListener { onBackPressed() }
        imageSave.setOnClickListener { saveNote() }

        // Set default color for note
        selectedNoteColor="#333333"

        initMiscellaneous()
        setSubtitleIndicatorColor()

        findViewById<View>(R.id.imageRemoveWebURL).setOnClickListener {
            textWebURL?.text = null
            layoutWebURL?.visibility = View.GONE
        }

        // Get note ID from intent extra
        val noteId = intent.getIntExtra("note_id", -1)

        // If noteId is not -1, then an existing note is being edited
        if (noteId != -1) {
            GlobalScope.launch(Dispatchers.IO) {
                alreadyAvailableNote = NotesDatabase.getNotesDatabase(this@CreateNoteActivity)
                    .noteDao()
                    .getNoteById(noteId)

                // Update the UI with the note details
                withContext(Dispatchers.Main) {
                    inputNoteTitle?.setText(alreadyAvailableNote?.title)
                    inputNoteSubtitle?.setText(alreadyAvailableNote?.subtitle)

                    // Load image if not null
                    if (alreadyAvailableNote?.imagePath != null) {
                        selectedImagePath = alreadyAvailableNote?.imagePath
                        imageNote?.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath))
                        imageNote?.visibility = VISIBLE
                    }

                    // Load web URL if not null
                    if (!alreadyAvailableNote?.webLink.isNullOrEmpty()) {
                        textWebURL?.text = alreadyAvailableNote?.webLink
                        layoutWebURL?.visibility = VISIBLE
                    }

                    // Load note text if not null
                    if (!alreadyAvailableNote?.noteText.isNullOrEmpty()) {
                        inputNoteText?.setText(alreadyAvailableNote?.noteText)
                    }
                }
            }
        }
    }

    // This function saves the note created by the user
    private fun saveNote() {

        // Get the title, subtitle and text of the note
        val noteTitle = inputNoteTitle?.text.toString().trim()
        val noteSubtitle = inputNoteSubtitle?.text.toString().trim()
        val noteText = inputNoteText?.text.toString().trim()

        // Check if the note title is empty
        if (noteTitle.isEmpty()) {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show()
            return
            // Check if both the note subtitle and text are empty
        } else if (noteSubtitle.isEmpty() && noteText.isEmpty()) {
            Toast.makeText(this, "Note can't be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new note object with the title, subtitle, text, date and color of the note
        val note = Note().apply {
            title = noteTitle
            subtitle = noteSubtitle
            this.noteText = noteText
            dateTime = textDateTime?.text?.toString()
            this.color = selectedNoteColor
            this.imagePath = selectedImagePath

        }

        // If a web URL was added, set it as the note's web link
        if (layoutWebURL?.visibility == VISIBLE) {
            note.webLink = textWebURL?.text.toString()
        }

        // If the note being edited already exists, set its ID to the new note's ID
        alreadyAvailableNote?.let { note.id = it.id }

        //// Creation of  an AsyncTask to insert the new note into the database
        class SaveNoteTask : AsyncTask<Void?, Void?, Void?>() {

            override fun doInBackground(vararg p0: Void?): Void? {
                NotesDatabase.getNotesDatabase(applicationContext).noteDao().insertNote(note)
                return null
            }


            override fun onPostExecute(aVoid: Void?) {
                super.onPostExecute(aVoid)
                // Set the result of the activity to RESULT_OK and finish it
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        SaveNoteTask().execute()

    }


    private fun initMiscellaneous() {
        val layoutMiscellaneous =
            findViewById<LinearLayout>(com.example.myapplication.R.id.layoutMiscellaneous)
        val bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous)
        layoutMiscellaneous.findViewById<TextView>(R.id.textMiscellaneous)
            .setOnClickListener {
                if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
                }
            }
        // Set up image views and their corresponding click listeners to select note color
        val imageColor1: ImageView =
            layoutMiscellaneous.findViewById(com.example.myapplication.R.id.imageColor1)
        val imageColor2: ImageView =
            layoutMiscellaneous.findViewById(com.example.myapplication.R.id.imageColor2)
        val imageColor3: ImageView =
            layoutMiscellaneous.findViewById(com.example.myapplication.R.id.imageColor3)
        val imageColor4: ImageView =
            layoutMiscellaneous.findViewById(com.example.myapplication.R.id.imageColor4)
        val imageColor5: ImageView =
            layoutMiscellaneous.findViewById(com.example.myapplication.R.id.imageColor5)

        layoutMiscellaneous.findViewById<View>(R.id.viewColor1).setOnClickListener {
            selectedNoteColor = "#333333"
            imageColor1.visibility = VISIBLE
            imageColor2.visibility = GONE
            imageColor3.visibility = GONE
            imageColor4.visibility = GONE
            imageColor5.visibility = GONE
            setSubtitleIndicatorColor()
        }

        layoutMiscellaneous.findViewById<View>(R.id.viewColor2).setOnClickListener {
            selectedNoteColor = "#FDBE3B"
            imageColor1.visibility = GONE
            imageColor2.visibility = VISIBLE
            imageColor3.visibility = GONE
            imageColor4.visibility = GONE
            imageColor5.visibility = GONE
            setSubtitleIndicatorColor()
        }

        layoutMiscellaneous.findViewById<View>(R.id.viewColor3).setOnClickListener {
            selectedNoteColor = "#FF4842"
            imageColor1.visibility = GONE
            imageColor2.visibility = GONE
            imageColor3.visibility = VISIBLE
            imageColor4.visibility = GONE
            imageColor5.visibility = GONE
            setSubtitleIndicatorColor()
        }

        layoutMiscellaneous.findViewById<View>(R.id.viewColor4).setOnClickListener {
            selectedNoteColor = "#3A52Fc"
            imageColor1.visibility = GONE
            imageColor2.visibility = GONE
            imageColor3.visibility = GONE
            imageColor4.visibility = VISIBLE
            imageColor5.visibility = GONE
            setSubtitleIndicatorColor()
        }

        layoutMiscellaneous.findViewById<View>(R.id.viewColor5).setOnClickListener {
            selectedNoteColor = "#000000"
            imageColor1.visibility = GONE
            imageColor2.visibility = GONE
            imageColor3.visibility = GONE
            imageColor4.visibility = GONE
            imageColor5.visibility = VISIBLE
            setSubtitleIndicatorColor()
        }


        val addImage: LinearLayout = layoutMiscellaneous.findViewById(R.id.layoutAddImage)

        // Set up click listener for adding image to note
        addImage.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@CreateNoteActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            } else {
                selectImage()
            }
        }

        layoutMiscellaneous.findViewById<View>(R.id.layoutAddUrl).setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            showAddURLDialog()
        }

        layoutMiscellaneous.findViewById<View>(R.id.layoutDeleteNote).setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            showDeleteNoteDialog()
        }


    }

    private fun setSubtitleIndicatorColor() {
        val gradientDrawable = viewSubtitleIndicator?.background as GradientDrawable
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor))
    }


    private fun selectImage() {
        startActivityForResult(
            createGetContentIntent(
                this,
                "image/*",
                title = "Select Image"
            ),
            REQUEST_CODE_SELECT_IMAGE
        )
    }
    //loads and save image to cache of the application
    fun loadAndSaveImageToCache(context: Context, uri: Uri?): String? {
        val bitmap = BitmapFactory.decodeFile(getRealPathFromURI(context, uri))
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return file.absolutePath
    }


    fun getRealPathFromURI(context: Context, uri: Uri?): String? {
        var filePath = ""
        val wholeID = DocumentsContract.getDocumentId(uri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":").toTypedArray()[1]
        val column = arrayOf(MediaStore.Images.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"
        val cursor: Cursor =
            context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, arrayOf(id), null) ?: return null
        val columnIndex: Int = cursor.getColumnIndex(column[0])
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex)
        }
        cursor.close()
        return filePath
    }

    fun createGetContentIntent(
        context: Context,
        type: String = "*/*",
        extraTypes: Array<String>? = null,
        title: String,
        allowMultiple: Boolean = false,
    ): Intent {
        //Document picker intent
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = type
        //extra types
        if (extraTypes != null) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, extraTypes)
        }
        //Allow multiple files to be selected
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        // Only return URIs that can be opened with ContentResolver
        intent.addCategory(Intent.CATEGORY_OPENABLE)


        //Samsung file manager intent
        val sIntent = Intent("com.sec.android.app.myfiles.PICK_DATA")
        sIntent.type = type
        // Only return URIs that can be opened with ContentResolver
        sIntent.addCategory(Intent.CATEGORY_OPENABLE)
        //Allow multiple files to be selected
        sIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)

        val chooserIntent: Intent
        if (context.packageManager.resolveActivity(sIntent, 0) != null) {
            //Device with Samsung file manager
            chooserIntent = Intent.createChooser(sIntent, title)
            //Combine the document picker intent so the user can choose
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intent))
        } else {
            chooserIntent = Intent.createChooser(intent, title)
        }

        return chooserIntent
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
            selectImage()
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Get references to views
        val removeImage = findViewById<ImageView>(R.id.imageRemoveImage)
        val imageNote: AppCompatImageView = findViewById(R.id.imageNote)

        // Check if the request code and result code indicate that an image was selected
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                // Get the Uri of the selected image
                val selectedImageUri = data.data
                if (selectedImageUri != null) {
                    try {
                        // Load the image from the Uri and save it to cache
                        val imagePath = loadAndSaveImageToCache(this, selectedImageUri)
                        // Create a Bitmap from the saved image
                        val bitmap = BitmapFactory.decodeFile(imagePath)
                        // Set the Bitmap as the imageNote ImageView's source
                        imageNote.setImageBitmap(bitmap)
                        // Make the imageNote and removeImage ImageViews visible
                        imageNote.visibility = VISIBLE
                        removeImage.visibility = VISIBLE
                        // Save the path of the selected image
                        selectedImagePath = imagePath
                    } catch (e: Exception) {
                        // Display an error message if the image couldn't be loaded or saved
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }





    private fun showAddURLDialog() {
        if (dialogAddURL == null) {
            val builder = AlertDialog.Builder(this@CreateNoteActivity)
            val view = LayoutInflater.from(this)
                .inflate(R.layout.layout_add_url, findViewById(R.id.layoutAddUrlContainer))
            builder.setView(view)
            dialogAddURL = builder.create()
            if (dialogAddURL?.window != null) {
                dialogAddURL?.window?.setBackgroundDrawable(ColorDrawable(0))
            }
            val inputURL = view.findViewById<EditText>(R.id.inputURL)
            inputURL.requestFocus()
            view.findViewById<View>(R.id.textAdd).setOnClickListener { _: View? ->
                val inputURLStr = inputURL.text.toString().trim { it <= ' ' }
                if (inputURLStr.isEmpty()) {
                    Toast.makeText(this@CreateNoteActivity, "Enter URL", Toast.LENGTH_SHORT).show()
                } else if (!Patterns.WEB_URL.matcher(inputURLStr).matches()) {
                    Toast.makeText(this@CreateNoteActivity, "Enter valid URL", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    textWebURL?.text = inputURL.text.toString()
                    layoutWebURL?.visibility = VISIBLE
                    dialogAddURL?.dismiss()
                }
            }
            view.findViewById<View>(R.id.textCancel)
                .setOnClickListener { dialogAddURL?.dismiss() }
        }
        dialogAddURL?.show()
    }


    private fun showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            val builder = AlertDialog.Builder(this@CreateNoteActivity)
            val view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, null)
            builder.setView(view)
            dialogDeleteNote = builder.create()
            if (dialogDeleteNote?.window != null) {
                dialogDeleteNote?.window?.setBackgroundDrawable(ColorDrawable(0))
            }
            view.findViewById<View>(R.id.textDeleteNote).setOnClickListener {
                @SuppressLint("StaticFieldLeak")
                class DeleteNoteTask : AsyncTask<Void?, Void?, Void?>() {
                    override fun doInBackground(vararg params: Void?): Void? {
                        NotesDatabase.getNotesDatabase(applicationContext).noteDao()
                            .deleteNote(alreadyAvailableNote)
                        return null
                    }

                    override fun onPostExecute(aVoid: Void?) {
                        super.onPostExecute(aVoid)
                        val intent = Intent()
                        intent.putExtra("isNoteDeleted", true)
                        setResult(RESULT_OK, intent)
                        dialogDeleteNote?.dismiss()
                        finish()
                    }
                }
                DeleteNoteTask().execute()
            }
        }
        dialogDeleteNote?.show()
    }


}






