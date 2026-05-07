package com.arthurgoupil.obsidianwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews

class WidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.arthurgoupil.obsidianwidget.ACTION_REFRESH"
        const val ACTION_OPEN_NOTE = "com.arthurgoupil.obsidianwidget.ACTION_OPEN_NOTE"
        const val ACTION_DATA_LOADED = "com.arthurgoupil.obsidianwidget.ACTION_DATA_LOADED"
        const val EXTRA_NOTE_URI = "extra_note_uri"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val prefs = PreferencesHelper(context)
                val tag = prefs.getWidgetTag(appWidgetId)

                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_header_text, "#$tag")

                // Set up the remote adapter for the list
                val serviceIntent = Intent(context, WidgetRemoteViewsService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.widget_list, serviceIntent)
                views.setEmptyView(R.id.widget_list, R.id.widget_empty_text)

                // Show loading spinner, hide list until data is ready
                views.setViewVisibility(R.id.widget_loading_container, View.VISIBLE)
                views.setViewVisibility(R.id.widget_list, View.GONE)
                views.setViewVisibility(R.id.widget_empty_text, View.GONE)

                // Click template: broadcast to this receiver, which will launch Obsidian
                val clickIntent = Intent(context, WidgetProvider::class.java).apply {
                    action = ACTION_OPEN_NOTE
                }
                val clickPendingIntent = PendingIntent.getBroadcast(
                    context, 0, clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent)

                // Obsidian logo: open Obsidian app
                val obsidianIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("obsidian://")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val obsidianPendingIntent = PendingIntent.getActivity(
                    context, 0, obsidianIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_obsidian_button, obsidianPendingIntent)

                // Search button: open SearchActivity
                val searchIntent = Intent(context, SearchActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val searchPendingIntent = PendingIntent.getActivity(
                    context, appWidgetId + 1000, searchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_search_button, searchPendingIntent)

                // Refresh button
                val refreshIntent = Intent(context, WidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
            } catch (e: Exception) {
                Log.e("ObsidianWidget", "Failed to update widget $appWidgetId", e)
            }
        }
        fun showListAfterLoad(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setViewVisibility(R.id.widget_loading_container, View.GONE)
                views.setViewVisibility(R.id.widget_list, View.VISIBLE)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("ObsidianWidget", "Failed to show list after load for $appWidgetId", e)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_DATA_LOADED -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    showListAfterLoad(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_REFRESH -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    // Show spinner over the existing list while refreshing
                    try {
                        val views = RemoteViews(context.packageName, R.layout.widget_layout)
                        views.setViewVisibility(R.id.widget_loading_container, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_list, View.GONE)
                        views.setViewVisibility(R.id.widget_empty_text, View.GONE)
                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                    } catch (e: Exception) {
                        Log.e("ObsidianWidget", "Failed to show spinner for refresh $appWidgetId", e)
                    }
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
                }
            }
            ACTION_OPEN_NOTE -> {
                val noteUriString = intent.getStringExtra(EXTRA_NOTE_URI) ?: return
                try {
                    val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(noteUriString)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(openIntent)
                } catch (e: Exception) {
                    Log.e("ObsidianWidget", "Failed to open note: $noteUriString", e)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = PreferencesHelper(context)
        for (id in appWidgetIds) {
            prefs.removeWidgetTag(id)
        }
    }
}
