package com.batodev.jigsawpuzzle.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.batodev.jigsawpuzzle.view.ImageAdapter
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.Settings
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


private const val CAMERA_PERMISSION_REQUEST_CODE = 1
private const val EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 2

private const val DIFF_SPLIT = "X"

class ImagePickActivity : AppCompatActivity() {
    private var photoUri: Uri? = null
    private var files: Array<String> = arrayOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_pick_activity)

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
            grid.onItemClickListener =
                OnItemClickListener { _: AdapterView<*>?, _: View?, itemClickedIndex: Int, _: Long ->
                    showStartGamePopup(itemClickedIndex, null)
                }
        } catch (e: IOException) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
        findViewById<AppCompatImageButton>(R.id.cameraButton).setOnClickListener {
            onImageFromCameraClick()
        }
        findViewById<AppCompatImageButton>(R.id.galleryButton).setOnClickListener {
            onImageFromGalleryClick()
        }
    }

    private fun showStartGamePopup(itemClickedIndex: Int?, mCurrentPhotoPath: String?) {
        val settings = SettingsHelper.load(this)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.start_game_popup, null)

        setUpDiffSpinner(popupView, settings)
        setUpCheckboxes(popupView, settings)

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(popupView)
        builder.setCancelable(true)

        val alertDialog = builder.create()
        alertDialog.show()
        val startButton = popupView.findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener { // Handle start button click
            startTheGame(
                itemClickedIndex,
                mCurrentPhotoPath,
                alertDialog
            )
        }

    }

    private fun setUpCheckboxes(popupView: View, settings: Settings) {
        val backImage = popupView.findViewById<CheckBox>(R.id.background_image_checkbox)
        backImage.setOnCheckedChangeListener { _, value ->
                settings.showImageInBackgroundOfThePuzzle = value
                SettingsHelper.save(this, settings)
            }
        backImage.isChecked = settings.showImageInBackgroundOfThePuzzle
        val backGrid = popupView.findViewById<CheckBox>(R.id.background_grid_checkbox)
        backGrid
            .setOnCheckedChangeListener { _, value ->
                settings.showGridInBackgroundOfThePuzzle = value
                SettingsHelper.save(this, settings)
            }
        backGrid.isChecked = settings.showGridInBackgroundOfThePuzzle
        val playSounds = popupView.findViewById<CheckBox>(R.id.play_sounds_checkbox)
        playSounds
            .setOnCheckedChangeListener { _, value ->
                settings.playSounds = value
                SettingsHelper.save(this, settings)
            }
        playSounds.isChecked = settings.playSounds
    }

    private fun setUpDiffSpinner(popupView: View, settings: Settings) {
        val dimensionsList = mutableListOf<String>()
        for (i in 3..11) {
            val dimension =
                "${i * (i + 2)} (${i}$DIFF_SPLIT${i + 2})" // Generate the dimension string
            dimensionsList.add(dimension) // Add it to the list
        }
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            dimensionsList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinner = popupView.findViewById<Spinner>(R.id.difficulty_spinner)
        val selectionFromSettings = "${settings.lastSetDifficultyCustomWidth * settings.lastSetDifficultyCustomHeight} (${settings.lastSetDifficultyCustomWidth}$DIFF_SPLIT${settings.lastSetDifficultyCustomHeight})"
        val indexOfSelection = dimensionsList.lastIndexOf(selectionFromSettings)
        spinner.adapter = adapter
        spinner.setSelection(indexOfSelection)
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                difficultyItemClickedIndex: Int,
                id: Long
            ) {
                diffClicked(dimensionsList[difficultyItemClickedIndex], settings)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun diffClicked(difficultyItemClicked: String, settings: Settings) {
        val split = difficultyItemClicked.substring(
            difficultyItemClicked.indexOf("(") + 1,
            difficultyItemClicked.indexOf(")")
        ).split(DIFF_SPLIT)
        settings.lastSetDifficultyCustomWidth = Integer.parseInt(split[0])
        settings.lastSetDifficultyCustomHeight = Integer.parseInt(split[1])
        SettingsHelper.save(this, settings)
    }

    private fun startTheGame(
        itemClickedIndex: Int?,
        mCurrentPhotoPath: String?,
        alertDialog: AlertDialog
    ) {
        val intent = Intent(applicationContext, PuzzleActivity::class.java)
        itemClickedIndex?.let {
            intent.putExtra("assetName", files[itemClickedIndex % files.size])
        }
        mCurrentPhotoPath?.let {
            intent.putExtra("mCurrentPhotoPath", it)
        }
        startActivity(intent)
        alertDialog.dismiss()
        finish()
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ar ->
        if (ar) {
            photoUri?.let {
                showStartGamePopup(null, photoUri.toString())
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setImageUri()
                cameraActivityResultLauncher.launch(photoUri)
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                Toast.makeText(this, "Exetrnal storage permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onImageFromCameraClick() {
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

    private fun copyFileAndStartGame(it: Uri?) {
        it?.let {
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
                    showStartGamePopup(null, pathToSave.toString())
                }
            }
        }
    }

    private val pickImageFromGallery = registerForActivityResult<PickVisualMediaRequest, Uri>(
        ActivityResultContracts.PickVisualMedia()
    ) {
        copyFileAndStartGame(it)
    }

    fun onImageFromGalleryClick() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            askForReadExternalImagesPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            askForReadExternalImagesPermission(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

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
