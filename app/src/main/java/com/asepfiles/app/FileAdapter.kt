package com.asepfiles.app

import android.content.Context
import android.graphics.Typeface
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileAdapter(
    private var items: List<FileItem>,
    private val ctx: Context,
    private val onClickListener: (FileItem) -> Unit,
    private val onLongClickListener: (FileItem) -> Boolean
) : RecyclerView.Adapter<FileAdapter.VH>() {

    private var selectedItems = mutableSetOf<String>()

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.file_icon)
        val name: TextView = view.findViewById(R.id.file_name)
        val info: TextView = view.findViewById(R.id.file_info)
        val checkBox: CheckBox = view.findViewById(R.id.file_checkbox)
        val card: View = view.findViewById(R.id.file_card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val fontSize = AppPrefs.getFontSize(ctx)
        val fontName = AppPrefs.getFont(ctx)

        val typeface = when(fontName) {
            "MONOSPACE" -> Typeface.MONOSPACE
            "SERIF" -> Typeface.SERIF
            "SANS_SERIF" -> Typeface.SANS_SERIF
            else -> Typeface.DEFAULT
        }

        holder.name.typeface = typeface
        holder.name.textSize = fontSize
        holder.info.typeface = typeface
        holder.info.textSize = fontSize - 3f

        holder.name.text = item.name
        holder.info.text = item.formattedSize()

        holder.icon.text = when {
            item.isDirectory -> "F"
            item.mimeType() == "image" -> "I"
            item.mimeType() == "video" -> "V"
            item.mimeType() == "audio" -> "A"
            item.mimeType() == "code" -> "C"
            item.mimeType() == "archive" -> "Z"
            item.mimeType() == "apk" -> "K"
            item.mimeType() == "pdf" -> "P"
            else -> "D"
        }

        val isSelected = selectedItems.contains(item.file.absolutePath)
        holder.checkBox.isChecked = isSelected
        holder.card.alpha = if (isSelected) 0.6f else 1f

        holder.itemView.setOnClickListener { onClickListener(item) }
        holder.itemView.setOnLongClickListener { onLongClickListener(item) }
        holder.checkBox.setOnClickListener {
            toggleSelection(item)
        }
    }

    fun toggleSelection(item: FileItem) {
        val path = item.file.absolutePath
        if (selectedItems.contains(path)) selectedItems.remove(path)
        else selectedItems.add(path)
        notifyDataSetChanged()
    }

    fun getSelectedFiles(): List<File> =
        items.filter { selectedItems.contains(it.file.absolutePath) }.map { it.file }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun hasSelection() = selectedItems.isNotEmpty()

    fun updateItems(newItems: List<FileItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}