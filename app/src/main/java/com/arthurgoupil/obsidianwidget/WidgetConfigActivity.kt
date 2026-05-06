package com.arthurgoupil.obsidianwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var prefs: PreferencesHelper
    private var selectedTag: String? = null

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
        if (vaultUri == null) {
            val messageText: TextView = findViewById(R.id.config_message)
            messageText.text = getString(R.string.config_no_vault)
            return
        }

        val scanner = NoteScanner(this)
        val tags = scanner.getAllTags(vaultUri)

        if (tags.isEmpty()) {
            val messageText: TextView = findViewById(R.id.config_message)
            messageText.text = getString(R.string.config_no_tags)
            return
        }

        val listView: ListView = findViewById(R.id.tags_list)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, tags)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedTag = tags[position]
        }

        val confirmButton: Button = findViewById(R.id.confirm_button)
        confirmButton.setOnClickListener {
            val tag = selectedTag
            if (tag == null) {
                Toast.makeText(this, getString(R.string.please_select_tag), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.setWidgetTag(appWidgetId, tag)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            WidgetProvider.updateWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
