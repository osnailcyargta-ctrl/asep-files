package com.asepfiles.app

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FileAdapter
    private lateinit var pathText: TextView
    private lateinit var toolbar: LinearLayout
    private lateinit var selectionToolbar: LinearLayout
    private lateinit var rootLayout: androidx.constraintlayout.widget.ConstraintLayout

    private var currentDir = Environment.getExternalStorageDirectory()
    private val backStack = ArrayDeque<File>()

    companion object { const val REQ_PERM = 300; const val REQ_WALLPAPER = 301 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.recycler)
        pathText = findViewById(R.id.path_text)
        toolbar = findViewById(R.id.toolbar)
        selectionToolbar = findViewById(R.id.selection_toolbar)
        rootLayout = findViewById(R.id.root_layout)

        recycler.layoutManager = LinearLayoutManager(this)

        applyWallpaper()
        setupToolbarButtons()
        setupSelectionButtons()
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        applyWallpaper()
        loadDir(currentDir)
    }

    private fun applyTheme() {
        val isDark = AppPrefs.isDarkMode(this)
        if (isDark) setTheme(R.style.Theme_AsepFiles_Dark)
        else setTheme(R.style.Theme_AsepFiles_Light)
    }

    private fun applyWallpaper() {
        val path = AppPrefs.getWallpaperPath(this) ?: return
        val file = File(path)
        if (!file.exists()) return
        try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bm = BitmapFactory.decodeFile(path, opts)
            rootLayout.background = android.graphics.drawable.BitmapDrawable(resources, bm)
        } catch (e: Exception) {}
    }

    private fun setupToolbarButtons() {
        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btn_new_folder).setOnClickListener {
            showNewFolderDialog()
        }
        findViewById<ImageButton>(R.id.btn_home).setOnClickListener {
            backStack.clear()
            loadDir(Environment.getExternalStorageDirectory())
        }
        findViewById<ImageButton>(R.id.btn_search).setOnClickListener {
            showSearchDialog()
        }
    }

    private fun setupSelectionButtons() {
        findViewById<Button>(R.id.btn_zip).setOnClickListener { zipSelected() }
        findViewById<Button>(R.id.btn_delete).setOnClickListener { deleteSelected() }
        findViewById<Button>(R.id.btn_copy).setOnClickListener { Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btn_cancel_sel).setOnClickListener {
            adapter.clearSelection()
            selectionToolbar.visibility = View.GONE
        }
    }

    private fun loadDir(dir: File) {
        currentDir = dir
        pathText.text = dir.absolutePath

        val showHidden = AppPrefs.showHidden(this)
        val sortBy = AppPrefs.getSortBy(this)

        var files = (dir.listFiles() ?: emptyArray())
            .filter { showHidden || !it.name.startsWith(".") }
            .map { FileItem(it) }

        files = when (sortBy) {
            "size" -> files.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.file.length() })
            "date" -> files.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.file.lastModified() })
            "type" -> files.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.extension })
            else -> files.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
        }

        adapter = FileAdapter(files, this,
            onClickListener = { item -> handleClick(item) },
            onLongClickListener = { item ->
                adapter.toggleSelection(item)
                selectionToolbar.visibility = View.VISIBLE
                true
            }
        )
        recycler.adapter = adapter
    }

    private fun handleClick(item: FileItem) {
        if (adapter.hasSelection()) {
            adapter.toggleSelection(item)
            return
        }
        if (item.isDirectory) {
            backStack.addLast(currentDir)
            loadDir(item.file)
        } else {
            when (item.mimeType()) {
                "code", "pdf" -> openCodeEditor(item.file)
                "archive" -> showUnzipDialog(item.file)
                "apk" -> installApk(item.file)
                else -> openFile(item.file)
            }
        }
    }

    private fun openCodeEditor(file: File) {
        val intent = Intent(this, CodeEditorActivity::class.java)
        intent.putExtra("file_path", file.absolutePath)
        startActivity(intent)
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mime = contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot install APK", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUnzipDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Extract ZIP")
            .setMessage("Extract ${file.name} here?")
            .setPositiveButton("Extract") { _, _ ->
                Thread {
                    try {
                        val destDir = File(file.parentFile, file.nameWithoutExtension)
                        ZipUtils.unzip(file, destDir)
                        runOnUiThread {
                            Toast.makeText(this, "Extracted to ${destDir.name}", Toast.LENGTH_SHORT).show()
                            loadDir(currentDir)
                        }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun zipSelected() {
        val selected = adapter.getSelectedFiles()
        if (selected.isEmpty()) return
        val name = if (selected.size == 1) selected[0].name else "archive_${System.currentTimeMillis()}"
        val outFile = File(currentDir, "$name.zip")
        Thread {
            try {
                if (selected.size == 1 && selected[0].isDirectory) {
                    ZipUtils.zipFolder(selected[0], outFile)
                } else {
                    val tmp = File(cacheDir, "zip_tmp_${System.currentTimeMillis()}").also { it.mkdirs() }
                    selected.forEach { it.copyRecursively(File(tmp, it.name), overwrite = true) }
                    ZipUtils.zipFolder(tmp, outFile)
                    tmp.deleteRecursively()
                }
                runOnUiThread {
                    Toast.makeText(this, "Zipped to ${outFile.name}", Toast.LENGTH_SHORT).show()
                    adapter.clearSelection()
                    selectionToolbar.visibility = View.GONE
                    loadDir(currentDir)
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun deleteSelected() {
        val selected = adapter.getSelectedFiles()
        AlertDialog.Builder(this)
            .setTitle("Delete ${selected.size} item(s)?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                selected.forEach { it.deleteRecursively() }
                adapter.clearSelection()
                selectionToolbar.visibility = View.GONE
                loadDir(currentDir)
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNewFolderDialog() {
        val input = EditText(this).apply { hint = "Folder name" }
        AlertDialog.Builder(this)
            .setTitle("New Folder")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    File(currentDir, name).mkdirs()
                    loadDir(currentDir)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply { hint = "Search file name..." }
        AlertDialog.Builder(this)
            .setTitle("Search in ${currentDir.name}")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim().lowercase()
                val results = currentDir.walkTopDown()
                    .filter { it.name.lowercase().contains(query) }
                    .map { FileItem(it) }.toList()
                adapter.updateItems(results)
                pathText.text = "Search: $query (${results.size} results)"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivityForResult(intent, REQ_PERM)
            } else loadDir(currentDir)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_PERM)
            } else loadDir(currentDir)
        }
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(req, perms, results)
        if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) loadDir(currentDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PERM) loadDir(currentDir)
    }

    override fun onBackPressed() {
        if (adapter.hasSelection()) {
            adapter.clearSelection()
            selectionToolbar.visibility = View.GONE
        } else if (backStack.isNotEmpty()) {
            loadDir(backStack.removeLast())
        } else super.onBackPressed()
    }
}