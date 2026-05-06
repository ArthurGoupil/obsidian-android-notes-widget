package com.arthurgoupil.obsidianwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
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
            return
        }

        val scanner = NoteScanner(context)
        notes = scanner.getNotesWithTag(vaultUri, tag)
    }

    override fun onDestroy() {
        notes = emptyList()
    }

    override fun getCount(): Int = notes.size

    override fun getViewAt(position: Int): RemoteViews {
        val note = notes[position]
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        views.setTextViewText(R.id.note_title, note.title)

        // Build the Obsidian URI with proper encoding
        val prefs = PreferencesHelper(context)
        val vaultName = prefs.vaultName
        val obsidianUri = android.net.Uri.Builder()
            .scheme("obsidian")
            .authority("open")
            .appendQueryParameter("vault", vaultName)
            .appendQueryParameter("file", note.relativePath)
            .build()

        val fillInIntent = Intent().apply {
            data = obsidianUri
        }
        views.setOnClickFillInIntent(R.id.note_title, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
