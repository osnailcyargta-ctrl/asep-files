package com.asepfiles.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    companion object { const val REQ_WALLPAPER = 401 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupFontPicker()
        setupFontSizeSlider()
        setupDarkModeToggle()
        setupWallpaperPicker()
        setupShowHiddenToggle()
        setupSortPicker()

        findViewById<ImageButton>(R.id.btn_back_settings).setOnClickListener { finish() }
    }

    private fun setupFontPicker() {
        val fonts = arrayOf("DEFAULT", "MONOSPACE", "SERIF", "SANS_SERIF")
        val spinner = findViewById<Spinner>(R.id.font_spinner)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fonts)
        spinner.setSelection(fonts.indexOf(AppPrefs.getFont(this)).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                AppPrefs.set(this@SettingsActivity, AppPrefs.KEY_FONT, fonts[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupFontSizeSlider() {
        val slider = findViewById<SeekBar>(R.id.font_size_slider)
        val label = findViewById<TextView>(R.id.font_size_label)
        slider.max = 20
        slider.progress = (AppPrefs.getFontSize(this) - 10f).toInt()
        label.text = "Font Size: ${AppPrefs.getFontSize(this).toInt()}sp"
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = (progress + 10).toFloat()
                label.text = "Font Size: ${size.toInt()}sp"
                AppPrefs.set(this@SettingsActivity, AppPrefs.KEY_FONT_SIZE, size)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun setupDarkModeToggle() {
        val toggle = findViewById<Switch>(R.id.dark_mode_toggle)
        toggle.isChecked = AppPrefs.isDarkMode(this)
        toggle.setOnCheckedChangeListener { _, checked ->
            AppPrefs.set(this, AppPrefs.KEY_DARK_MODE, checked)
            Toast.makeText(this, "Restart app to apply theme", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWallpaperPicker() {
        val btn = findViewById<Button>(R.id.btn_pick_wallpaper)
        val btnClear = findViewById<Button>(R.id.btn_clear_wallpaper)
        btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQ_WALLPAPER)
        }
        btnClear.setOnClickListener {
            AppPrefs.set(this, AppPrefs.KEY_WALLPAPER_PATH, "")
            Toast.makeText(this, "Wallpaper cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupShowHiddenToggle() {
        val toggle = findViewById<Switch>(R.id.show_hidden_toggle)
        toggle.isChecked = AppPrefs.showHidden(this)
        toggle.setOnCheckedChangeListener { _, checked ->
            AppPrefs.set(this, AppPrefs.KEY_SHOW_HIDDEN, checked)
        }
    }

    private fun setupSortPicker() {
        val sorts = arrayOf("name", "size", "date", "type")
        val labels = arrayOf("Name", "Size", "Date", "Type")
        val spinner = findViewById<Spinner>(R.id.sort_spinner)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinner.setSelection(sorts.indexOf(AppPrefs.getSortBy(this)).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                AppPrefs.set(this@SettingsActivity, AppPrefs.KEY_SORT_BY, sorts[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_WALLPAPER && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val inStream = contentResolver.openInputStream(uri) ?: return
                val file = File(filesDir, "wallpaper.jpg")
                FileOutputStream(file).use { inStream.copyTo(it) }
                AppPrefs.set(this, AppPrefs.KEY_WALLPAPER_PATH, file.absolutePath)
                Toast.makeText(this, "Wallpaper set!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}