package com.batodev.jigsawpuzzle

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


private const val CUSTOM = "Custom"
private const val HARD = "Hard"
private const val MEDIUM = "Medium"


private const val CAMERA_PERMISSION_REQUEST_CODE = 1
private const val EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 2

class ImagePickActivity : AppCompatActivity() {
    private var photoUri: Uri? = null
    private var files: Array<String> = arrayOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_pick_activity)
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
    }

    private fun showStartGamePopup(itemClickedIndex: Int?, mCurrentPhotoPath: String?) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.start_game_popup, null)
        val radioGroup = popupView.findViewById<RadioGroup>(R.id.radioGroup)
        val dropdownHeight = popupView.findViewById<Spinner>(R.id.dropdown_height)
        val dropdownWidth = popupView.findViewById<Spinner>(R.id.dropdown_width)
        val adapter: ArrayAdapter<Int> = ArrayAdapter<Int>(
            this,
            android.R.layout.simple_spinner_item,
            listOf(3, 4, 5, 6, 7, 8, 9)
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dropdownHeight.adapter = adapter
        dropdownWidth.adapter = adapter
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(popupView)
        builder.setCancelable(true)

        val settings = SettingsHelper.load(this)
        val customRadioButton = popupView.findViewById<RadioButton>(R.id.customRadioButton)
        when (settings.lastSetDifficulty) {
            EASY -> {
                popupView.findViewById<RadioButton>(R.id.easyRadioButton).isChecked = true
                easy(dropdownHeight, dropdownWidth)
            }

            MEDIUM -> {
                popupView.findViewById<RadioButton>(R.id.mediumRadioButton).isChecked = true
                medium(dropdownHeight, dropdownWidth)
            }

            HARD -> {
                popupView.findViewById<RadioButton>(R.id.hardRadioButton).isChecked = true
                hard(dropdownHeight, dropdownWidth)
            }

            CUSTOM -> {
                customRadioButton.isChecked = true
                custom(dropdownHeight, dropdownWidth, popupView, settings)
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.customRadioButton -> {
                    custom(dropdownHeight, dropdownWidth, popupView, settings)
                }

                R.id.easyRadioButton -> {
                    easy(dropdownHeight, dropdownWidth)
                }

                R.id.mediumRadioButton -> {
                    medium(dropdownHeight, dropdownWidth)
                }

                R.id.hardRadioButton -> {
                    hard(dropdownHeight, dropdownWidth)
                }
            }
        }
        val alertDialog = builder.create()
        alertDialog.show()
        val startButton = popupView.findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener { // Handle start button click
            startTheGame(
                customRadioButton,
                itemClickedIndex,
                mCurrentPhotoPath,
                dropdownWidth,
                dropdownHeight,
                settings,
                popupView,
                alertDialog
            )
        }
        popupView.findViewById<CheckBox>(R.id.background_image_checkbox)
            .setOnCheckedChangeListener { _, value ->
                settings.showImageInBackgroundOfThePuzzle = value
                SettingsHelper.save(this, settings)
            }
    }

    private fun startTheGame(
        customRadioButton: RadioButton,
        itemClickedIndex: Int?,
        mCurrentPhotoPath: String?,
        dropdownWidth: Spinner,
        dropdownHeight: Spinner,
        settings: Settings,
        popupView: View,
        alertDialog: AlertDialog
    ) {
        val customChecked = customRadioButton.isChecked
        val intent = Intent(applicationContext, PuzzleActivity::class.java)
        itemClickedIndex?.let {
            intent.putExtra("assetName", files[itemClickedIndex % files.size])
        }
        mCurrentPhotoPath?.let {
            intent.putExtra("mCurrentPhotoPath", it)
        }
        intent.putExtra("width", Integer.valueOf(dropdownWidth.selectedItem.toString()))
        intent.putExtra("height", Integer.valueOf(dropdownHeight.selectedItem.toString()))
        startActivity(intent)
        if (customChecked) {
            settings.lastSetDifficultyCustomWidth = dropdownWidth.selectedItemId.toInt()
            settings.lastSetDifficultyCustomHeight = dropdownHeight.selectedItemId.toInt()
        }
        if (popupView.findViewById<RadioButton>(R.id.easyRadioButton).isChecked) {
            settings.lastSetDifficulty = EASY
        }
        if (popupView.findViewById<RadioButton>(R.id.mediumRadioButton).isChecked) {
            settings.lastSetDifficulty = MEDIUM
        }
        if (popupView.findViewById<RadioButton>(R.id.hardRadioButton).isChecked) {
            settings.lastSetDifficulty = HARD
        }
        if (customChecked) {
            settings.lastSetDifficulty = CUSTOM
        }
        SettingsHelper.save(this, settings)
        alertDialog.dismiss()
        finish()
    }

    private fun custom(
        dropdownHeight: Spinner,
        dropdownWidth: Spinner,
        popupView: View,
        settings: Settings,
    ) {
        dropdownHeight.isEnabled = true
        dropdownWidth.isEnabled = true
        popupView.findViewById<Spinner>(R.id.dropdown_width)
            .setSelection(settings.lastSetDifficultyCustomWidth)
        popupView.findViewById<Spinner>(R.id.dropdown_height)
            .setSelection(settings.lastSetDifficultyCustomHeight)
    }

    private fun hard(
        dropdownHeight: Spinner,
        dropdownWidth: Spinner,
    ) {
        dropdownHeight.isEnabled = false
        dropdownWidth.isEnabled = false
        dropdownHeight.setSelection(3)
        dropdownWidth.setSelection(2)
    }

    private fun medium(
        dropdownHeight: Spinner,
        dropdownWidth: Spinner,
    ) {
        dropdownHeight.isEnabled = false
        dropdownWidth.isEnabled = false
        dropdownHeight.setSelection(2)
        dropdownWidth.setSelection(1)
    }

    private fun easy(
        dropdownHeight: Spinner,
        dropdownWidth: Spinner,
    ) {
        dropdownHeight.isEnabled = false
        dropdownWidth.isEnabled = false
        dropdownHeight.setSelection(1)
        dropdownWidth.setSelection(0)
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

    fun onImageFromCameraClick(view: View?) {
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
        );
    }

    private fun copyFileAndStartGame(it: Uri?) {
        it?.let {
            contentResolver.openFileDescriptor(it, "r").use { parcelFileDescriptor ->
                val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
                val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                val pathToSave = File(File(filesDir, "camera_images"), "temp.jpg")
                FileOutputStream(pathToSave).use {
                    image.compress(Bitmap.CompressFormat.JPEG, 90, it)
                }
                showStartGamePopup(null, pathToSave.toString())
            }
        }
    }

    private var pickImageFromGallery = registerForActivityResult<PickVisualMediaRequest, Uri>(
        ActivityResultContracts.PickVisualMedia()
    ) {
        copyFileAndStartGame(it)
    }

    fun onImageFromGalleryClick(view: View?) {
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

    companion object {
        const val EASY = "Easy"
    }
}
