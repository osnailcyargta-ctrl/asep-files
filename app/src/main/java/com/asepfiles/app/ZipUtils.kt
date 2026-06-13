package com.asepfiles.app

import java.io.*
import java.util.zip.*

object ZipUtils {

    fun zipFolder(folder: File, outputZip: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
            addFolderToZip(folder, folder.name, zos)
        }
    }

    private fun addFolderToZip(file: File, path: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addFolderToZip(child, "$path/${child.name}", zos)
            }
        } else {
            zos.putNextEntry(ZipEntry(path))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    fun unzip(zipFile: File, destDir: File) {
        destDir.mkdirs()
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}