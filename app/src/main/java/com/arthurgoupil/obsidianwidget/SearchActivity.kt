package com.arthurgoupil.obsidianwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class SearchActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private var allNotes: List<Note> = emptyList()
    private var filteredNotes: List<Note> = emptyList()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var prefs: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        prefs = PreferencesHelper(this)

        val searchInput: EditText = findViewById(R.id.search_input)
        val closeButton: ImageButton = findViewById(R.id.search_close_button)
        val resultsList: ListView = findViewById(R.id.search_results_list)
        val emptyText: TextView = findViewById(R.id.search_empty_text)
        val progressBar: ProgressBar = findViewById(R.id.search_progress)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        resultsList.adapter = adapter

        closeButton.setOnClickListener { finish() }

        // Tap a result to open in Obsidian
        resultsList.setOnItemClickListener { _, _, position, _ ->
            val note = filteredNotes.getOrNull(position) ?: return@setOnItemClickListener
            openNote(note)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterNotes(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Load all notes in background
        val vaultUri = prefs.vaultUri
        if (vaultUri == null) {
            emptyText.text = getString(R.string.config_no_vault)
            return
        }

        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        executor.execute {
            val notes = try {
                NoteScanner(this).getAllNotes(vaultUri)
            } catch (e: Exception) {
                Log.e("ObsidianWidget", "Failed to load notes for search", e)
                emptyList()
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                allNotes = notes
                emptyText.visibility = View.VISIBLE
                emptyText.text = getString(R.string.search_prompt)
                filterNotes(searchInput.text.toString())
            }
        }
    }

    private fun filterNotes(query: String) {
        val resultsList: ListView = findViewById(R.id.search_results_list)
        val emptyText: TextView = findViewById(R.id.search_empty_text)

        val filtered = if (query.isBlank()) {
            emptyList()
        } else {
            allNotes.filter { it.title.contains(query, ignoreCase = true) }
        }

        filteredNotes = filtered
        adapter.clear()
        adapter.addAll(filtered.map { it.title })
        adapter.notifyDataSetChanged()

        if (query.isBlank()) {
            emptyText.text = getString(R.string.search_prompt)
            emptyText.visibility = View.VISIBLE
            resultsList.visibility = View.GONE
        } else if (filtered.isEmpty()) {
            emptyText.text = "No notes matching \"$query\""
            emptyText.visibility = View.VISIBLE
            resultsList.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            resultsList.visibility = View.VISIBLE
        }
    }

    private fun openNote(note: Note) {
        try {
            val filePath = note.relativePath.stripVaultPrefix(prefs.vaultName)
            val uri = Uri.Builder()
                .scheme("obsidian")
                .authority("open")
                .appendQueryParameter("vault", prefs.vaultName)
                .appendQueryParameter("file", filePath)
                .build()
            startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Log.e("ObsidianWidget", "Failed to open note: ${note.title}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}
