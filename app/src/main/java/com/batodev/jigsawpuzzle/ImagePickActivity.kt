package com.batodev.jigsawpuzzle

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.Date


private const val CUSTOM = "Custom"

private const val HARD = "Hard"

private const val MEDIUM = "Medium"


class ImagePickActivity : Activity() {
    private var files: Array<String> = arrayOf()
    private var mCurrentPhotoPath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        val am = assets
        try {
            files = am.list("img") ?: arrayOf()
            val grid = findViewById<GridView>(R.id.grid)
            grid.adapter = ImageAdapter(this)
            grid.onItemClickListener =
                OnItemClickListener { _: AdapterView<*>?, _: View?, itemClickedIndex: Int, _: Long ->
                    showStartGamePopup(itemClickedIndex)
                }
        } catch (e: IOException) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStartGamePopup(itemClickedIndex: Int) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.start_game_popup, null)
        val radioGroup = popupView.findViewById<RadioGroup>(R.id.radioGroup)
        val dropdownHeight = popupView.findViewById<Spinner>(R.id.dropdown_height)
        val dropdownWidth = popupView.findViewById<Spinner>(R.id.dropdown_width)
        val adapter: ArrayAdapter<Int> = ArrayAdapter<Int>(this, android.R.layout.simple_spinner_item, listOf(3,4,5,6,7,8,9))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dropdownHeight.adapter = adapter
        dropdownWidth.adapter = adapter
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(popupView)
        builder.setCancelable(true)

        val settings = SettingsHelper.load(this)
        val customRadioButton = popupView.findViewById<RadioButton>(R.id.customRadioButton)
        when(settings.lastSetDifficulty) {
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
            val intent = Intent(
                applicationContext, PuzzleActivity::class.java
            )
            intent.putExtra("assetName", files[itemClickedIndex % files.size])
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

    fun onImageFromCameraClick(view: View?) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (e: IOException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
        if (photoFile != null) {
            val photoUri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // permission not granted, initiate request
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
            )
        } else {
            // Create an image file name
            val timeStamp = DateFormat.getDateTimeInstance().format(Date())
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                storageDir /* directory */
            )
            mCurrentPhotoPath = image.absolutePath // save this to use in the intent
            return image
        }
        return null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onImageFromCameraClick(View(this))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val intent = Intent(this, PuzzleActivity::class.java)
            intent.putExtra("mCurrentPhotoPath", mCurrentPhotoPath)
            startActivity(intent)
        }
        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK) {
            val uri = data.data
            val intent = Intent(this, PuzzleActivity::class.java)
            intent.putExtra("mCurrentPhotoUri", uri.toString())
            startActivity(intent)
        }
    }

    fun onImageFromGalleryClick(view: View?) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE
            )
        } else {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
        }
    }

    companion object {
        val EASY = "Easy"
        private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2
        private const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 3
        const val REQUEST_IMAGE_GALLERY = 4
    }
}
