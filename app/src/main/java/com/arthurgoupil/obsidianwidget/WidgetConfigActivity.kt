package com.arthurgoupil.obsidianwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var prefs: PreferencesHelper
    private var selectedTag: String? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)

        setResult(RESULT_CANCELED)

        prefs = PreferencesHelper(this)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val vaultUri = prefs.vaultUri
        if (vaultUri == null || !hasUriPermission(vaultUri)) {
            prefs.vaultUri = null
            findViewById<TextView>(R.id.config_message).text = getString(R.string.config_no_vault)
            findViewById<Button>(R.id.confirm_button).visibility = View.GONE
            return
        }

        // Show loading state while scanning tags in background
        val messageText: TextView = findViewById(R.id.config_message)
        val listView: ListView = findViewById(R.id.tags_list)
        val confirmButton: Button = findViewById(R.id.confirm_button)
        val progressBar: ProgressBar = findViewById(R.id.loading_progress)

        messageText.text = getString(R.string.loading_tags)
        progressBar.visibility = View.VISIBLE
        confirmButton.visibility = View.GONE

        executor.execute {
            val tags = try {
                val scanner = NoteScanner(this)
                scanner.getAllTags(vaultUri)
            } catch (e: Exception) {
                Log.e("ObsidianWidget", "Failed to scan tags", e)
                emptyList()
            }

            runOnUiThread {
                progressBar.visibility = View.GONE

                if (tags.isEmpty()) {
                    messageText.text = getString(R.string.config_no_tags)
                    return@runOnUiThread
                }

                messageText.text = getString(R.string.select_tag_description)
                confirmButton.visibility = View.VISIBLE

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_single_choice,
                    tags
                )
                listView.adapter = adapter
                listView.choiceMode = ListView.CHOICE_MODE_SINGLE

                listView.setOnItemClickListener { _, _, position, _ ->
                    selectedTag = tags[position]
                }

                confirmButton.setOnClickListener {
                    val tag = selectedTag
                    if (tag == null) {
                        Toast.makeText(
                            this,
                            getString(R.string.please_select_tag),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    try {
                        prefs.setWidgetTag(appWidgetId, tag)
                        val appWidgetManager = AppWidgetManager.getInstance(this)
                        WidgetProvider.updateWidget(this, appWidgetManager, appWidgetId)
                    } catch (e: Exception) {
                        Log.e("ObsidianWidget", "Failed to update widget", e)
                    }

                    val resultValue = Intent().putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId
                    )
                    setResult(RESULT_OK, resultValue)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    private fun hasUriPermission(uri: android.net.Uri): Boolean {
        return contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    }
}
