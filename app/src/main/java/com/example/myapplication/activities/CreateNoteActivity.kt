package com.example.myapplication.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.util.UUID

class CreateNoteActivity : AppCompatActivity() {
    // ----------------- VARIABLES -----------------
    companion object {
        private const val TAG = "CreateNoteActivity"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    // UI Elements - Note Content
    private var inputNoteTitle: EditText? = null
    private var inputNoteSubtitle: EditText? = null
    private var inputNoteText: EditText? = null
    private var textDateTime: TextView? = null
    private var viewSubtitleIndicator: View? = null

    // UI Elements - Miscellaneous
    private var selectedNoteColor: String? = "#333333"

    // UI Elements - Image Handling
    private var selectedImagePath: String? = null
    private var removeImage: ImageView? = null
    private lateinit var selectedImage: ImageView
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>

    // UI Elements - Web URL
    private var textWebURL: TextView? = null
    private var layoutWebURL: LinearLayout? = null

    private var alreadyAvailableNote: Note? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        initializeViews()
        setupListeners()
        checkGalleryPermissions()
        setCurrentDateTime()
        initMiscellaneous()
        handleExistingNote()
        setupImageSelection()
    }

    /**
     * Initializes all UI view references from the layout.
     * Gets references to all EditText, TextView, ImageView and other UI components.
     */
    private fun initializeViews() {
        // Note Content Views
        inputNoteTitle = findViewById(R.id.inputNoteTitle)
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle)
        inputNoteText = findViewById(R.id.inputNoteText)
        textDateTime = findViewById(R.id.textDateTime)
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator)

        // Web URL Views
        textWebURL = findViewById(R.id.textWebURL)
        layoutWebURL = findViewById(R.id.layoutWebURL)

        // Image Views
        removeImage = findViewById(R.id.imageRemoveImage)
        selectedImage = findViewById(R.id.selectedImage)
    }

    /**
     * Sets up click listeners for all interactive UI elements.
     * Includes back/save buttons, image handling buttons, and back press handling.
     */
    private fun setupListeners() {
        val imageBack = findViewById<ImageView>(R.id.imageBack)
        val imageSave = findViewById<ImageView>(R.id.imageSave)

        // Set up click listeners
        findViewById<View>(R.id.layoutAddImage).setOnClickListener { openGallery() }
        removeImage?.setOnClickListener { removeSelectedImage() }
        findViewById<View>(R.id.imageRemoveWebURL).setOnClickListener { removeWebUrl() }

        // Back handling
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = saveNote()
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
        imageSave.setOnClickListener { saveNote() }
        imageBack.setOnClickListener { backCallback.handleOnBackPressed() }
    }

    /**
     * Sets the current date and time in the note's timestamp field.
     * Uses SimpleDateFormat to format the current date and time in a readable format.
     */
    private fun setCurrentDateTime() {
        textDateTime?.text = SimpleDateFormat(
            "EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()
        ).format(Date())
    }

    // ----------------- NOTE CONTENT METHODS -----------------
    /**
     * Validates and saves the current note.
     * Checks for required fields (title and at least one content field),
     * then creates a Note object and saves it to the database.
     */
    private fun saveNote() {
        val noteTitle = inputNoteTitle?.text.toString().trim()
        val noteSubtitle = inputNoteSubtitle?.text.toString().trim()
        val noteText = inputNoteText?.text.toString().trim()

        when {
            noteTitle.isEmpty() -> Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show()
            noteSubtitle.isEmpty() && noteText.isEmpty() && selectedImagePath.isNullOrEmpty() ->
                Toast.makeText(this, "Content required", Toast.LENGTH_SHORT).show()
            else -> saveNoteToDatabase(Note().apply {
                title = noteTitle
                subtitle = noteSubtitle
                this.noteText = noteText
                imagePath = selectedImagePath
                dateTime = textDateTime?.text?.toString()
                color = selectedNoteColor
                webLink = textWebURL?.text?.takeIf { layoutWebURL?.visibility == VISIBLE }?.toString()
                alreadyAvailableNote?.id?.let { id = it }
            })
        }
    }

    /**
     * Saves a note to the database.
     * Uses coroutines to perform database operation on a background thread,
     * then returns to the main thread to finish the activity.
     *
     * @param note The Note object to be saved to the database
     */
    private fun saveNoteToDatabase(note: Note) {
        lifecycleScope.launch(Dispatchers.IO) {
            NotesDatabase.getNotesDatabase(applicationContext).noteDao().insertNote(note)
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    /**
     * Checks if the activity was launched to edit an existing note.
     * If so, retrieves the note data from the database and populates the UI fields.
     * Handles loading of text content, image, and web URL if present.
     */
    private fun handleExistingNote() {
        intent.getIntExtra("note_id", -1).takeIf { it != -1 }?.let { noteId ->
            lifecycleScope.launch(Dispatchers.IO) {
                NotesDatabase.getNotesDatabase(this@CreateNoteActivity).noteDao()
                    .getNoteById(noteId)?.let { note ->
                        withContext(Dispatchers.Main) {
                            alreadyAvailableNote = note
                            inputNoteTitle?.setText(note.title)
                            inputNoteSubtitle?.setText(note.subtitle)
                            inputNoteText?.setText(note.noteText)

                            // Load image if available
                            note.imagePath?.let {
                                selectedImagePath = it
                                try {
                                    val file = File(it)
                                    if (file.exists()) {
                                        val contentUri = getContentUriFromFilePath(it)
                                        displaySelectedImage(contentUri)
                                        removeImage?.visibility = VISIBLE
                                    } else {
                                        Log.e(TAG, "Image file does not exist: $it")
                                        Toast.makeText(
                                            this@CreateNoteActivity,
                                            "Image file not found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error loading existing image: ${e.message}", e)
                                    Toast.makeText(
                                        this@CreateNoteActivity,
                                        "Error loading image",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            // Load web link if available
                            note.webLink?.let {
                                textWebURL?.text = it
                                layoutWebURL?.visibility = VISIBLE
                            }
                        }
                    }
            }
        }
    }

    // ----------------- MISCELLANEOUS/COLOR METHODS -----------------
    /**
     * Initializes the miscellaneous options bottom sheet.
     * Sets up the color selection options, URL addition, and note deletion functions.
     * Configures the bottom sheet behavior for expanding and collapsing.
     */
    private fun initMiscellaneous() {
        val layoutMiscellaneous = findViewById<LinearLayout>(R.id.layoutMiscellaneous)
        val bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous)

        layoutMiscellaneous.findViewById<TextView>(R.id.textMiscellaneous).setOnClickListener {
            bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
                else -> BottomSheetBehavior.STATE_EXPANDED
            }
        }

        // Color selection setup
        val colors = listOf(
            R.id.viewColor1 to "#333333",
            R.id.viewColor2 to "#FDBE3B",
            R.id.viewColor3 to "#FF4842",
            R.id.viewColor4 to "#3A52Fc",
            R.id.viewColor5 to "#000000"
        )

        colors.forEach { (viewId, color) ->
            layoutMiscellaneous.findViewById<View>(viewId).setOnClickListener {
                selectedNoteColor = color
                updateColorIndicators(viewId)
                setSubtitleIndicatorColor()
            }
        }

        // Other miscellaneous options
        listOf(R.id.layoutAddUrl, R.id.layoutDeleteNote).forEach { viewId ->
            layoutMiscellaneous.findViewById<View>(viewId).setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                when (viewId) {
                    R.id.layoutAddUrl -> showAddUrlDialog()
                    R.id.layoutDeleteNote -> showDeleteDialog()
                }
            }
        }
    }

    /**
     * Updates the subtitle indicator color based on the currently selected note color.
     * Uses the GradientDrawable of the indicator to set its fill color.
     */
    private fun setSubtitleIndicatorColor() {
        (viewSubtitleIndicator?.background as? GradientDrawable)?.setColor(
            Color.parseColor(selectedNoteColor)
        )
    }

    /**
     * Updates the color selection indicators in the UI.
     * Shows a check mark on the currently selected color and hides it on others.
     *
     * @param selectedViewId The resource ID of the selected color view
     */
    private fun updateColorIndicators(selectedViewId: Int) {
        val imageColor1: ImageView = findViewById(R.id.imageColor1)
        val imageColor2: ImageView = findViewById(R.id.imageColor2)
        val imageColor3: ImageView = findViewById(R.id.imageColor3)
        val imageColor4: ImageView = findViewById(R.id.imageColor4)
        val imageColor5: ImageView = findViewById(R.id.imageColor5)

        // Create a mapping of imageColor IDs to their corresponding image views
        val colorMap = mapOf(
            R.id.viewColor1 to imageColor1,
            R.id.viewColor2 to imageColor2,
            R.id.viewColor3 to imageColor3,
            R.id.viewColor4 to imageColor4,
            R.id.viewColor5 to imageColor5
        )

        // Update visibility for all image views
        colorMap.values.forEach { it.visibility = GONE }
        colorMap[selectedViewId]?.visibility = VISIBLE
    }

    /**
     * Shows a confirmation dialog for deleting the current note.
     * If confirmed, deletes the note from the database and returns to the previous activity.
     * Only available when editing an existing note.
     */
    private fun showDeleteDialog() {
        // Create dialog builder
        val builder = AlertDialog.Builder(this)

        // Properly inflate the layout with a parent view for layout params (false = don't attach to parent)
        val view = LayoutInflater.from(this@CreateNoteActivity)
            .inflate(R.layout.layout_delete_note, findViewById(android.R.id.content), false)

        builder.setView(view)

        // Create the dialog before setting click listeners
        val dialog = builder.create()

        // Set up delete confirmation
        view.findViewById<View>(R.id.textDeleteNote).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                alreadyAvailableNote?.let {
                    NotesDatabase.getNotesDatabase(applicationContext).noteDao().deleteNote(it)
                    withContext(Dispatchers.Main) {
                        setResult(RESULT_OK, Intent().putExtra("isNoteDeleted", true))
                        dialog.dismiss()
                        finish()
                    }
                }
            }
        }

        dialog.show() // Show the dialog after setup
    }

    // ----------------- IMAGE HANDLING METHODS -----------------
    /**
     * Sets up the activity result launcher for image selection.
     * Handles the result from gallery selection, processes the selected image,
     * and saves it to app storage before displaying it.
     */
    private fun setupImageSelection() {
        selectImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        try {
                            Log.d(TAG, "Got URI: $uri")

                            // Create a copy of the image in the app's files directory
                            val imagePath = copyImageToAppStorage(uri)
                            if (imagePath != null) {
                                selectedImagePath = imagePath
                                // Convert to content URI for display
                                val contentUri = getContentUriFromFilePath(imagePath)
                                displaySelectedImage(contentUri)
                                removeImage?.visibility = VISIBLE
                            } else {
                                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing result: ${e.message}", e)
                            Toast.makeText(
                                this,
                                "Error processing image: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } ?: run {
                        Log.e(TAG, "No data returned from picker")
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG, "Selection cancelled or failed, resultCode: ${result.resultCode}")
                }
            }
    }

    /**
     * Opens the device gallery for image selection.
     * Creates an intent to select images from gallery and launches it through the activity launcher.
     * Includes error handling for devices without gallery access.
     */
    private fun openGallery() {
        try {
            // Try multiple intent approaches to increase compatibility
            val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

            // Create a chooser to give user more options
            val chooserIntent = Intent.createChooser(galleryIntent, "Select Picture")

            // Launch the chooser
            selectImageLauncher.launch(chooserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening gallery: ${e.message}", e)
            Toast.makeText(this, "Could not open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies an image from the provided URI to app's private storage.
     * Creates a unique filename and copies the image data to the new file.
     *
     * @param uri The URI of the image to copy
     * @return The absolute file path of the copied image, or null if copying failed
     */
    private fun copyImageToAppStorage(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                // Create a unique filename
                val fileName = "image_${UUID.randomUUID()}.jpg"
                val outputFile = File(filesDir, fileName)

                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                Log.d(TAG, "Image saved to app storage: ${outputFile.absolutePath}")
                outputFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying image to app storage: ${e.message}", e)
            null
        }
    }

    /**
     * Displays an image from the given URI in the image view.
     * Handles different Android versions by using appropriate image loading methods.
     * Includes fallback to direct file loading if content URI method fails.
     *
     * @param uri The URI of the image to display
     */
    private fun displaySelectedImage(uri: Uri) {
        try {
            Log.d(TAG, "Displaying image from: $uri")

            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            }

            selectedImage.setImageBitmap(bitmap)
            selectedImage.visibility = VISIBLE
            removeImage?.visibility = VISIBLE

            Log.d(TAG, "Successfully displayed image")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying image: ${e.message}", e)

            // Try direct file loading
            try {
                if (selectedImagePath != null) {
                    val file = File(selectedImagePath!!)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            selectedImage.setImageBitmap(bitmap)
                            selectedImage.visibility = VISIBLE
                            removeImage?.visibility = VISIBLE
                            return
                        }
                    }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Error in fallback display method: ${e2.message}", e2)
            }

            Toast.makeText(this, "Error displaying image: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Removes the currently selected image.
     * Clears the image view, removes the image file from storage,
     * and resets the image path variable.
     */
    private fun removeSelectedImage() {
        selectedImage.setImageBitmap(null)
        selectedImage.visibility = GONE

        // Delete the file
        selectedImagePath?.let {
            try {
                val file = File(it)
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Deleted image file: $it")
                } else {
                    Log.d(TAG, "Image file not found: $it")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image file: ${e.message}", e)
            }
        }

        selectedImagePath = null
        removeImage?.visibility = GONE
    }

    /**
     * Converts a file path to a content URI using FileProvider.
     * This is necessary for sharing files with other apps and for displaying images safely.
     *
     * @param filePath The absolute path to the file
     * @return A content URI for the file
     */
    private fun getContentUriFromFilePath(filePath: String): Uri {
        val file = File(filePath)
        return FileProvider.getUriForFile(
            this,
            "${packageName}.imageprovider",
            file
        )
    }

    /**
     * Checks and requests permissions needed for accessing the gallery.
     * Handles different permission requirements for different Android versions.
     * Uses READ_MEDIA_IMAGES for Android 13+ and READ_EXTERNAL_STORAGE for older versions.
     */
    private fun checkGalleryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    /**
     * Handles the result of a permission request.
     * Shows appropriate toast messages based on whether permission was granted or denied.
     *
     * @param requestCode The request code passed to requestPermissions
     * @param permissions The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permissions granted, now you can access gallery
            Toast.makeText(
                this,
                "Permission granted, you can now select images",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, "Permission denied, cannot access gallery", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // ----------------- WEB URL METHODS -----------------
    /**
     * Shows a dialog for adding a web URL to the note.
     * Validates the entered URL using patterns and updates the note if valid.
     * Displays appropriate error messages for empty or invalid URLs.
     */
    private fun showAddUrlDialog() {
        // Create dialog builder
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this@CreateNoteActivity)
            .inflate(R.layout.layout_add_url, findViewById(R.id.layoutAddUrlContainer), false)
        builder.setView(view)

        // Create the dialog before setting up click listeners
        val dialog = builder.create()

        val inputUrl = view.findViewById<EditText>(R.id.inputURL)

        // Positive button click
        view.findViewById<View>(R.id.textAdd).setOnClickListener {
            val url = inputUrl.text.toString().trim()
            when {
                url.isEmpty() -> Toast.makeText(
                    this@CreateNoteActivity,
                    "Enter URL",
                    Toast.LENGTH_SHORT
                ).show()

                !Patterns.WEB_URL.matcher(url).matches() -> Toast.makeText(
                    this@CreateNoteActivity,
                    "Invalid URL",
                    Toast.LENGTH_SHORT
                ).show()

                else -> {
                    textWebURL?.text = url
                    layoutWebURL?.visibility = VISIBLE
                    dialog.dismiss()
                }
            }
        }

        // Negative button click
        view.findViewById<View>(R.id.textCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show() // Show the dialog after setup
    }

    /**
     * Removes the web URL from the current note.
     * Clears the URL text and hides the URL layout.
     */
    private fun removeWebUrl() {
        textWebURL?.text = null
        layoutWebURL?.visibility = GONE
    }
}