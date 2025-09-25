package com.batodev.jigsawpuzzle.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.GridView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.FirebaseHelper
import com.batodev.jigsawpuzzle.helpers.NeonBtnOnPressChangeLook
import com.batodev.jigsawpuzzle.helpers.Settings
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import com.batodev.jigsawpuzzle.view.ImageAdapter
import com.smb.glowbutton.NeonButton
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


private const val CAMERA_PERMISSION_REQUEST_CODE = 1
private const val EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 2

private const val DIFF_SPLIT = "X"

enum class PhotoSource {
    CAMERA, GALLERY
}
/**
 * An activity for picking an image from the gallery or camera to use in the puzzle.
 */
class ImagePickActivity : AppCompatActivity() {
    private var photoUri: Uri? = null
    private var files: Array<String> = arrayOf()

    /**
     * Called when the activity is first created.
     * Initializes the UI, sets up image selection grid, and configures camera and gallery buttons.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_pick_activity)
        FirebaseHelper.logScreenView(this, "ImagePickActivity")

        val windowInsetsController = WindowCompat.getInsetsController(this.window, this.window.decorView)
        windowInsetsController.let { controller ->
            // Hide both bars
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Sticky behavior - bars stay hidden until user swipes
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        supportActionBar?.hide()
        val am = assets
        try {
            files = am.list("img") ?: arrayOf()
            val grid = findViewById<GridView>(R.id.grid)
            grid.adapter = ImageAdapter(this)
            grid.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, itemClickedIndex: Int, _: Long ->
                FirebaseHelper.logEvent(this, "image_picked_from_grid")
                showStartGamePopup(itemClickedIndex, null, PhotoSource.CAMERA)
            }
        } catch (e: IOException) {
            FirebaseHelper.logException(this, "onCreate", e.message)
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
        findViewById<AppCompatImageButton>(R.id.cameraButton).setOnClickListener {
            onImageFromCameraClick()
        }
        findViewById<AppCompatImageButton>(R.id.galleryButton).setOnClickListener {
            onImageFromGalleryClick()
        }
    }

    /**
     * Displays a popup dialog to configure game settings before starting the puzzle.
     * @param itemClickedIndex The index of the clicked image in the grid, or null if from camera/gallery.
     * @param mCurrentPhotoPath The file path of the selected image from camera/gallery, or null if from assets.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showStartGamePopup(
        itemClickedIndex: Int?,
        mCurrentPhotoPath: String?,
        photoSource: PhotoSource
    ) {
        Log.d(ImagePickActivity::class.simpleName, "showStartGamePopup: $itemClickedIndex, $mCurrentPhotoPath")
        val settings = SettingsHelper.load(this)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.start_game_popup, null)


        setUpDiffSpinner(popupView, settings)
        setUpCheckboxes(popupView, settings)

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(popupView)
        builder.setCancelable(true)

        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        alertDialog.show()
        val startButton = popupView.findViewById<NeonButton>(R.id.startButton)
        startButton.setOnClickListener { // Handle start button click
            startTheGame(
                itemClickedIndex,
                mCurrentPhotoPath,
                alertDialog,
                photoSource
            )
        }
        startButton.setOnTouchListener { view, event ->
            NeonBtnOnPressChangeLook.neonBtnOnPressChangeLook(view, event, this@ImagePickActivity)
            true
        }
    }

    /**
     * Sets up the checkboxes in the game start popup for various game settings.
     * @param popupView The inflated view of the game start popup.
     * @param settings The current {@link Settings} object to bind and save changes to.
     * @see Settings
     * @see SettingsHelper
     */
    private fun setUpCheckboxes(popupView: View, settings: Settings) {
        val backImage = popupView.findViewById<CheckBox>(R.id.background_image_checkbox)
        backImage.setOnCheckedChangeListener { _, value ->
            FirebaseHelper.logEvent(this, "checkbox_background_image", Bundle().apply { putBoolean("checked", value) })
            settings.showImageInBackgroundOfThePuzzle = value
            SettingsHelper.save(this, settings)
        }
        backImage.isChecked = settings.showImageInBackgroundOfThePuzzle
        val backGrid = popupView.findViewById<CheckBox>(R.id.background_grid_checkbox)
        backGrid.setOnCheckedChangeListener { _, value ->
            FirebaseHelper.logEvent(this, "checkbox_background_grid", Bundle().apply { putBoolean("checked", value) })
            settings.showGridInBackgroundOfThePuzzle = value
            SettingsHelper.save(this, settings)
        }
        backGrid.isChecked = settings.showGridInBackgroundOfThePuzzle
        val playSounds = popupView.findViewById<CheckBox>(R.id.play_sounds_checkbox)
        playSounds.setOnCheckedChangeListener { _, value ->
            FirebaseHelper.logEvent(this, "checkbox_play_sounds", Bundle().apply { putBoolean("checked", value) })
            settings.playSounds = value
            SettingsHelper.save(this, settings)
        }
        playSounds.isChecked = settings.playSounds
    }

    /**
     * Sets up the difficulty spinner in the game start popup.
     * @param popupView The inflated view of the game start popup.
     * @param settings The current {@link Settings} object to retrieve and save difficulty settings.
     * @see Settings
     * @see SettingsHelper
     */
    private fun setUpDiffSpinner(popupView: View, settings: Settings) {
        val dimensionsList = mutableListOf<String>()
        for (i in 2..11) {
            val dimension = "${i * (i + 2)} (${i}$DIFF_SPLIT${i + 2})" // Generate the dimension string
            dimensionsList.add(dimension) // Add it to the list
        }
        // Use the custom layout R.layout.custom_spinner_item
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            R.layout.custom_spinner_item, // Changed from android.R.layout.simple_spinner_item
            dimensionsList
        )
        // You might also want a custom layout for the dropdown view if the default
        // android.R.layout.simple_spinner_dropdown_item doesn't look right with white text.
        // If so, create another layout file (e.g., custom_spinner_dropdown_item.xml)
        // and set its text color to white, then use:
        // adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        // For now, we'll keep the default dropdown item layout.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinner = popupView.findViewById<Spinner>(R.id.difficulty_spinner)
        val selectionFromSettings = "${settings.lastSetDifficultyCustomWidth * settings.lastSetDifficultyCustomHeight} (${settings.lastSetDifficultyCustomWidth}$DIFF_SPLIT${settings.lastSetDifficultyCustomHeight})"
        val indexOfSelection = dimensionsList.lastIndexOf(selectionFromSettings)
        spinner.adapter = adapter
        spinner.setSelection(indexOfSelection)
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, // The AdapterView where the selection happened
                view: View?, // The view within the AdapterView that was clicked
                difficultyItemClickedIndex: Int, // The position of the view in the adapter
                id: Long // The row id of the item that is selected
            ) {
                diffClicked(dimensionsList[difficultyItemClickedIndex], settings)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Another interface callback
            }
        }
    }

    /**
     * Handles the selection of a difficulty level from the spinner.
     * Updates the puzzle dimensions in {@link Settings}.
     * @param difficultyItemClicked The string representation of the selected difficulty.
     * @param settings The current {@link Settings} object to update.
     * @see Settings
     * @see SettingsHelper
     */
    private fun diffClicked(difficultyItemClicked: String, settings: Settings) {
        FirebaseHelper.logEvent(this, "difficulty_changed", Bundle().apply { putString("difficulty", difficultyItemClicked) })
        val split = difficultyItemClicked.substring(
            difficultyItemClicked.indexOf("(") + 1,
            difficultyItemClicked.indexOf(")")
        ).split(DIFF_SPLIT)
        settings.lastSetDifficultyCustomWidth = Integer.parseInt(split[0])
        settings.lastSetDifficultyCustomHeight = Integer.parseInt(split[1])
        SettingsHelper.save(this, settings)
    }

    /**
     * Starts the {@link PuzzleActivity} with the selected image and dismisses the dialog.
     * @param itemClickedIndex The index of the clicked image in the grid, or null.
     * @param mCurrentPhotoPath The file path of the selected image from camera/gallery, or null.
     * @param alertDialog The AlertDialog to be dismissed after starting the game.
     * @see PuzzleActivity
     */
    private fun startTheGame(
        itemClickedIndex: Int?,
        mCurrentPhotoPath: String?,
        alertDialog: AlertDialog,
        photoSource: PhotoSource
    ) {
        FirebaseHelper.logButtonClick(this, "start_game")
        val intent = Intent(applicationContext, PuzzleActivity::class.java)
        intent.putExtra("newGame", true)
        itemClickedIndex?.let {
            intent.putExtra("assetName", files[itemClickedIndex % files.size])
        }
        mCurrentPhotoPath?.let {
            intent.putExtra("mCurrentPhotoPath", it)
        }
        intent.putExtra(PhotoSource::class.simpleName, photoSource.toString())
        startActivity(intent)
        alertDialog.dismiss()
        finish()
    }

    /**
     * Activity result launcher for capturing an image using the camera.
     * Handles the result of the camera capture and proceeds to show the game start popup.
     */
    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ar ->
        if (ar) {
            photoUri?.let {
                FirebaseHelper.logEvent(this, "image_from_camera_success")
                showStartGamePopup(null, photoUri.toString(), PhotoSource.CAMERA)
            }
        } else {
            FirebaseHelper.logEvent(this, "image_from_camera_canceled")
        }
    }

    /**
     * Callback for the result of requesting permissions.
     * Handles camera and external storage permission requests.
     * @param requestCode The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FirebaseHelper.logEvent(this, "camera_permission_granted")
                setImageUri()
                cameraActivityResultLauncher.launch(photoUri)
            } else {
                FirebaseHelper.logEvent(this, "camera_permission_denied")
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FirebaseHelper.logEvent(this, "storage_permission_granted")
                pickImageFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                FirebaseHelper.logEvent(this, "storage_permission_denied")
                Toast.makeText(this, "Exetrnal storage permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Initiates the process of capturing an image from the camera.
     * Requests camera permission if not already granted.
     */
    fun onImageFromCameraClick() {
        FirebaseHelper.logButtonClick(this, "image_from_camera")
        if (checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                listOf(Manifest.permission.CAMERA).toTypedArray(),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            setImageUri()
            cameraActivityResultLauncher.launch(photoUri)
        }
    }

    /**
     * Sets the URI for the image captured by the camera.
     * Creates a directory for camera images if it doesn't exist.
     */
    private fun setImageUri() {
        val directory = File(filesDir, "camera_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        photoUri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".fileprovider",
            File(directory, "temp.jpg")
        )
    }

    /**
     * Copies the selected image from the gallery to a temporary file and starts the game.
     * @param it The URI of the selected image.
     * @throws IOException if an I/O error occurs during file copying.
     */
    private fun copyFileAndStartGame(it: Uri?, photoSource: PhotoSource) {
        it?.let {
            try {
                contentResolver.openFileDescriptor(it, "r").use { parcelFileDescriptor ->
                    val directory = File(filesDir, "camera_images")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val pathToSave = File(directory, "temp.jpg")
                    parcelFileDescriptor?.fileDescriptor?.let { fd ->
                        val inputStream = FileInputStream(fd)
                        val outputStream = FileOutputStream(pathToSave)
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        outputStream.close()
                        inputStream.close()
                        showStartGamePopup(null, pathToSave.toString(), photoSource)
                    }
                }
            } catch (e: IOException) {
                FirebaseHelper.logException(this, "copyFileAndStartGame", e.message)
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Activity result launcher for picking an image from the gallery.
     * Handles the result of the gallery selection and proceeds to copy the file and show the game start popup.
     */
    private val pickImageFromGallery = registerForActivityResult<PickVisualMediaRequest, Uri>(
        ActivityResultContracts.PickVisualMedia()
    ) {
        if (it != null) {
            FirebaseHelper.logEvent(this, "image_from_gallery_success")
            copyFileAndStartGame(it, PhotoSource.GALLERY)
        } else {
            FirebaseHelper.logEvent(this, "image_from_gallery_canceled")
        }
    }

    /**
     * Initiates the process of picking an image from the gallery.
     * Requests appropriate external storage permissions based on Android version.
     */
    fun onImageFromGalleryClick() {
        FirebaseHelper.logButtonClick(this, "image_from_gallery")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            askForReadExternalImagesPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            askForReadExternalImagesPermission(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    /**
     * Requests the specified external storage permission.
     * @param readExternalStorage The permission string to request (e.g., {@link Manifest.permission#READ_EXTERNAL_STORAGE}).
     */
    private fun askForReadExternalImagesPermission(readExternalStorage: String) {
        if (checkSelfPermission(
                readExternalStorage
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(readExternalStorage),
                EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
            )
        } else {
            pickImageFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
