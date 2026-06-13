package com.asepfiles.app

import android.graphics.Typeface
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class CodeEditorActivity : AppCompatActivity() {

    private lateinit var editor: EditText
    private lateinit var lineNumbers: TextView
    private lateinit var fileName: TextView
    private var file: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_editor)

        editor = findViewById(R.id.code_editor)
        lineNumbers = findViewById(R.id.line_numbers)
        fileName = findViewById(R.id.editor_filename)

        val path = intent.getStringExtra("file_path")
        if (path != null) {
            file = File(path)
            fileName.text = file!!.name
            try {
                val content = file!!.readText()
                editor.setText(content)
                updateLineNumbers(content)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot read file", Toast.LENGTH_SHORT).show()
            }
        }

        // Apply font settings
        val fontName = AppPrefs.getFont(this)
        val fontSize = AppPrefs.getFontSize(this)
        val typeface = when (fontName) {
            "MONOSPACE" -> Typeface.MONOSPACE
            "SERIF" -> Typeface.SERIF
            else -> Typeface.MONOSPACE // default monospace for code
        }
        editor.typeface = typeface
        editor.textSize = fontSize
        lineNumbers.typeface = typeface
        lineNumbers.textSize = fontSize

        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateLineNumbers(s.toString())
            }
        })

        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveFile() }
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btn_undo).setOnClickListener {
            Toast.makeText(this, "Ctrl+Z not supported yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLineNumbers(text: String) {
        val lines = text.split("\n").size
        lineNumbers.text = (1..lines).joinToString("\n") { it.toString() }
    }

    private fun saveFile() {
        file?.let {
            try {
                it.writeText(editor.text.toString())
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}