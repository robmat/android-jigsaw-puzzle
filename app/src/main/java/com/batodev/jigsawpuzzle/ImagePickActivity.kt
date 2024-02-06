package com.batodev.jigsawpuzzle

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


private const val CUSTOM = "Custom"
private const val HARD = "Hard"
private const val MEDIUM = "Medium"
private const val CAMERA_REQUEST = 1888


class ImagePickActivity : AppCompatActivity() {
    private var imageUri: Uri? = null
    private var photoTakenFilePath: Uri? = null
    private var files: Array<String> = arrayOf()
    private var mCurrentPhotoPath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_pick_activity)
        supportActionBar?.hide();
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
            val customChecked = customRadioButton.isChecked
            val intent = Intent(applicationContext, PuzzleActivity::class.java)
            itemClickedIndex?.let {
                intent.putExtra("assetName", files[itemClickedIndex % files.size])
            }
            mCurrentPhotoPath?.let {
                intent.putExtra("mCurrentPhotoPath", mCurrentPhotoPath)
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
    ActivityResultContracts.StartActivityForResult()
    ) {
        imageUri?.let {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(it, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            val pathToSave = File(filesDir.absolutePath,"temp.jpg")
            FileOutputStream(pathToSave).use {
                image.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            showStartGamePopup(null, pathToSave.toString())
        }
    };


    fun onImageFromCameraClick(view: View?) {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
        ) {
            val permission =
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permission, 112)
        }
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(cameraIntent)
    }

    fun onImageFromGalleryClick(view: View?) {

    }

    companion object {
        val EASY = "Easy"
        private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2
        private const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 3
        const val REQUEST_IMAGE_GALLERY = 4
    }
}
