package com.arthurgoupil.obsidianwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class WidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetRemoteViewsFactory(applicationContext, intent)
    }
}

class WidgetRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var notes: List<Note> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val prefs = PreferencesHelper(context)
        val vaultUri = prefs.vaultUri
        val tag = prefs.getWidgetTag(appWidgetId)
        if (vaultUri == null || tag.isEmpty()) {
            notes = emptyList()
        } else {
            notes = try {
                val scanner = NoteScanner(context)
                scanner.getNotesWithTag(vaultUri, tag)
            } catch (e: Exception) {
                Log.e("ObsidianWidget", "Failed to scan notes for widget $appWidgetId", e)
                emptyList()
            }
        }

        // Notify the widget to hide the spinner and show the list
        val intent = Intent(context, WidgetProvider::class.java).apply {
            action = WidgetProvider.ACTION_DATA_LOADED
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        context.sendBroadcast(intent)
    }

    override fun onDestroy() {
        notes = emptyList()
    }

    override fun getCount(): Int = notes.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)

        val currentNotes = notes
        if (position >= currentNotes.size) {
            views.setTextViewText(R.id.note_title, "")
            return views
        }

        val note = currentNotes[position]
        views.setTextViewText(R.id.note_title, note.title)

        try {
            val prefs = PreferencesHelper(context)
            val vaultName = prefs.vaultName
            val filePath = note.relativePath.stripVaultPrefix(vaultName)
            val obsidianUri = android.net.Uri.Builder()
                .scheme("obsidian")
                .authority("open")
                .appendQueryParameter("vault", vaultName)
                .appendQueryParameter("file", filePath)
                .build()
                .toString()

            val fillInIntent = Intent().apply {
                putExtra(WidgetProvider.EXTRA_NOTE_URI, obsidianUri)
            }
            views.setOnClickFillInIntent(R.id.note_item_root, fillInIntent)
        } catch (e: Exception) {
            Log.e("ObsidianWidget", "Failed to set click intent for note: ${note.title}", e)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews {
        // Return a blank item so Android doesn't show its own "Chargement" placeholders
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        views.setTextViewText(R.id.note_title, "")
        return views
    }
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
