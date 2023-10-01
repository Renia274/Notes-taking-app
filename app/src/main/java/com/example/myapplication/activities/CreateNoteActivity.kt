package com.example.myapplication.activities



import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.database.NotesDatabase
import com.example.myapplication.entities.Note
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class CreateNoteActivity : AppCompatActivity() {
    private var inputNoteTitle: EditText? = null
    private var requestCodeTakeImage = 3
    private var inputNoteSubtitle: EditText? = null
    private var inputNoteText: EditText? = null
    private var textDateTime: TextView? = null
    private var viewSubtitleIndicator: View? = null
    private var imageNote: ImageView? = null
    private var textWebURL: TextView? = null
    private var layoutWebURL: LinearLayout? = null
    private var selectedNoteColor: String? = null
    private val requestCodeStoragePermission = 1
    private val requestCodeSelectImage = 2
    private var dialogAddURL: AlertDialog? = null
    private var dialogDeleteNote: AlertDialog? = null
    private var alreadyAvailableNote: Note? = null
    private var selectedImagePath: String? = null
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private var removeImage: ImageView? = null
    private lateinit var openAppSettingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickMultipleMedia: ActivityResultLauncher<PickVisualMediaRequest>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)
        // Get references to UI elements
        val imageBack = findViewById<ImageView>(R.id.imageBack)
        val imageSave = findViewById<ImageView>(R.id.imageSave)
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
        // Set default color for note
        selectedNoteColor = "#333333"
        initMiscellaneous()
        setSubtitleIndicatorColor()
        findViewById<View>(R.id.imageRemoveWebURL).setOnClickListener {
            textWebURL?.text = null
            layoutWebURL?.visibility = GONE
        }
        val noteId = intent.getIntExtra("note_id", -1)
        // If noteId is not -1, then an existing note is being edited
        if (noteId != -1) {
            lifecycleScope.launch(Dispatchers.IO) {
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
                        removeImage?.visibility = VISIBLE
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

        // Initialize the pickMultipleMedia launcher
        pickMultipleMedia =
            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
                if (uris.isNotEmpty()) {
                    Log.d("PhotoPicker", "Number of items selected: ${uris.size}")
                    // Handle the selected URIs here
                    if (uris.size == 1) {
                        // Only one item selected, process it (e.g., display, save, or use it)
                        val selectedUri = uris[0]
                        handleSelectedMedia(selectedUri)
                    } else {
                        // Multiple items selected, inform the user to select only one
                        Toast.makeText(this@CreateNoteActivity, "Select only one media item", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }



        findViewById<View>(R.id.layoutAddImage).setOnClickListener {
            // Launch the media picker
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
        // Register the onBackPressedCallback here
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button press here
                saveNote()
            }
        }
        // Register the callback
        onBackPressedDispatcher.addCallback(this, callback)
        // Set onClickListener for imageSave ImageView
        imageSave.setOnClickListener { saveNote() }
        // Set onClickListener for imageBack ImageView
        imageBack.setOnClickListener {
            // Call the callback to handle the back button press
            callback.handleOnBackPressed()
        }
        selectImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    if (data != null) {
                        // Handle the selected image here
                        val selectedImageUri = data.data
                        if (selectedImageUri != null) {
                            try {
                                val imagePath = loadAndSaveImageToCache(this, selectedImageUri)
                                val bitmap = BitmapFactory.decodeFile(imagePath)
                                imageNote?.setImageBitmap(bitmap)
                                imageNote?.visibility = VISIBLE
                                removeImage?.visibility = VISIBLE
                                selectedImagePath = imagePath
                            } catch (e: Exception) {
                                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else if (result.resultCode == Activity.RESULT_CANCELED) {
                    // Handle image selection cancellation here
                    Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun handleSelectedMedia(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // Process the input stream (e.g., decode it to a bitmap)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageNote?.setImageBitmap(bitmap)
                imageNote?.visibility = VISIBLE
                removeImage?.visibility = VISIBLE
                selectedImagePath = uri.toString() // Store the URI as a string
            } else {
                Toast.makeText(this, "Failed to open input stream for the selected media", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing selected media: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // After processing the media, you can save the note here or in any other appropriate place.
        saveNote()
    }



    // This function saves the note created by the user
    private fun saveNote() {
        // Get the title, subtitle, and text of the note
        val noteTitle = inputNoteTitle?.text.toString().trim()
        val noteSubtitle = inputNoteSubtitle?.text.toString().trim()
        val noteText = inputNoteText?.text.toString().trim()
        // Check if the note title is empty
        if (noteTitle.isEmpty()) {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show()
            return
        } else if (noteSubtitle.isEmpty() && noteText.isEmpty()) {
            Toast.makeText(this, "Note can't be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        // Create a new note object with the title, subtitle, text, date, and color of the note
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
        lifecycleScope.launch(Dispatchers.IO) {
            NotesDatabase.getNotesDatabase(applicationContext).noteDao().insertNote(note)
            withContext(Dispatchers.Main) {
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        }
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
    //loads and save image to cache of the application
    private fun loadAndSaveImageToCache(context: Context, uri: Uri?): String? {
        val bitmap = BitmapFactory.decodeFile(getRealPathFromURI(context, uri))
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return file.absolutePath
    }
    private fun getRealPathFromURI(context: Context, uri: Uri?): String? {
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
    private fun createGetContentIntent(
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
                lifecycleScope.launch(Dispatchers.IO) {
                    // Perform database deletion operation in the background
                    NotesDatabase.getNotesDatabase(applicationContext).noteDao()
                        .deleteNote(alreadyAvailableNote)
                    // Notify the UI thread that the note is deleted
                    withContext(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putExtra("isNoteDeleted", true)
                        setResult(Activity.RESULT_OK, intent)
                        dialogDeleteNote?.dismiss()
                        finish()
                    }
                }
            }
        }
        dialogDeleteNote?.show()
    }
}
