package com.arthurgoupil.obsidianwidget

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.BufferedReader
import java.io.InputStreamReader

data class Note(
    val title: String,
    val relativePath: String
)

/** Strip "vaultName/" prefix from a path if present (handles wrong parent-folder selection). */
fun String.stripVaultPrefix(vaultName: String): String {
    val prefix = "$vaultName/"
    return if (startsWith(prefix, ignoreCase = true)) removePrefix(prefix) else this
}

class NoteScanner(private val context: Context) {

    fun getNotesWithTag(vaultUri: Uri, tag: String): List<Note> {
        val notes = mutableListOf<Note>()
        val treeDocId = DocumentsContract.getTreeDocumentId(vaultUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(vaultUri, treeDocId)
        scanDir(vaultUri, childrenUri, "", tag, notes)
        return notes.distinctBy { it.relativePath }.sortedBy { it.title.lowercase() }
    }

    fun getAllNotes(vaultUri: Uri): List<Note> {
        val notes = mutableListOf<Note>()
        val treeDocId = DocumentsContract.getTreeDocumentId(vaultUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(vaultUri, treeDocId)
        collectAllNotes(vaultUri, childrenUri, "", notes)
        return notes.distinctBy { it.relativePath }.sortedBy { it.title.lowercase() }
    }

    private fun collectAllNotes(treeUri: Uri, dirUri: Uri, currentPath: String, notes: MutableList<Note>) {
        val cursor = try {
            context.contentResolver.query(
                dirUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            ) ?: return
        } catch (_: SecurityException) { return }
        catch (_: IllegalArgumentException) { return }

        cursor.use {
            while (it.moveToNext()) {
                try {
                    val docId = it.getString(0) ?: continue
                    val name = it.getString(1) ?: continue
                    val mimeType = it.getString(2) ?: ""

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                        val childPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                        collectAllNotes(treeUri, childUri, childPath, notes)
                    } else if (name.endsWith(".md")) {
                        val title = name.removeSuffix(".md")
                        val relativePath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                        notes.add(Note(title, relativePath))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun getAllTags(vaultUri: Uri): List<String> {
        val tags = mutableSetOf<String>()
        val treeDocId = DocumentsContract.getTreeDocumentId(vaultUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(vaultUri, treeDocId)
        collectTagsFromDir(vaultUri, childrenUri, tags)
        return tags.sorted()
    }

    private fun collectTagsFromDir(treeUri: Uri, dirUri: Uri, tags: MutableSet<String>) {
        val cursor = try {
            context.contentResolver.query(
                dirUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            ) ?: return
        } catch (_: SecurityException) { return }
        catch (_: IllegalArgumentException) { return }

        cursor.use {
            while (it.moveToNext()) {
                try {
                    val docId = it.getString(0) ?: continue
                    val name = it.getString(1) ?: continue
                    val mimeType = it.getString(2) ?: ""

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                        collectTagsFromDir(treeUri, childUri, tags)
                    } else if (name.endsWith(".md")) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        tags.addAll(readTagsFromFile(docUri))
                    }
                } catch (_: Exception) {
                    // Skip problematic entries
                }
            }
        }
    }

    private fun scanDir(
        treeUri: Uri,
        dirUri: Uri,
        currentPath: String,
        tag: String,
        notes: MutableList<Note>
    ) {
        val cursor = try {
            context.contentResolver.query(
                dirUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            ) ?: return
        } catch (_: SecurityException) { return }
        catch (_: IllegalArgumentException) { return }

        cursor.use {
            while (it.moveToNext()) {
                try {
                    val docId = it.getString(0) ?: continue
                    val name = it.getString(1) ?: continue
                    val mimeType = it.getString(2) ?: ""

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                        val childPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                        scanDir(treeUri, childUri, childPath, tag, notes)
                    } else if (name.endsWith(".md")) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val fileTags = readTagsFromFile(docUri)
                        if (fileTags.any { t -> t.equals(tag, ignoreCase = true) }) {
                            val title = name.removeSuffix(".md")
                            val relativePath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                            notes.add(Note(title, relativePath))
                        }
                    }
                } catch (_: Exception) {
                    // Skip problematic entries
                }
            }
        }
    }

    private fun readTagsFromFile(uri: Uri): List<String> {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val firstLine = reader.readLine()?.trim() ?: return emptyList()
                if (firstLine != "---") return emptyList()

                val frontmatter = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.trim() == "---") break
                    frontmatter.appendLine(line)
                }

                return parseTags(frontmatter.toString())
            }
        } catch (_: Exception) {
            // Skip files that can't be read
        }
        return emptyList()
    }

    private fun parseTags(frontmatter: String): List<String> {
        val tags = mutableListOf<String>()
        val lines = frontmatter.lines()
        var inTagsBlock = false

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("tags:")) {
                val inline = trimmed.removePrefix("tags:").trim()
                if (inline.isNotEmpty()) {
                    // tags: [a, b] or tags: a, b
                    val cleaned = inline
                        .removePrefix("[").removeSuffix("]")
                    tags.addAll(
                        cleaned.split(",")
                            .map { it.trim().removePrefix("#").trim('"', '\'', ' ') }
                            .filter { it.isNotEmpty() }
                    )
                    return tags
                }
                inTagsBlock = true
                continue
            }

            if (inTagsBlock) {
                if (trimmed.startsWith("- ")) {
                    val tagValue = trimmed.removePrefix("- ").trim()
                        .removePrefix("#")
                        .trim('"', '\'', ' ')
                    if (tagValue.isNotEmpty()) {
                        tags.add(tagValue)
                    }
                } else if (trimmed.isNotEmpty()) {
                    break
                }
            }
        }

        return tags
    }
}
