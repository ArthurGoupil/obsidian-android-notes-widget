package com.arthurgoupil.obsidianwidget

import android.content.Context
import android.net.Uri

class PreferencesHelper(context: Context) {

    private val prefs = context.getSharedPreferences("obsidian_widget_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_VAULT_URI = "vault_uri"
        private const val KEY_VAULT_NAME = "vault_name"
        private const val KEY_WIDGET_TAG_PREFIX = "widget_tag_"
    }

    var vaultUri: Uri?
        get() = prefs.getString(KEY_VAULT_URI, null)?.let { Uri.parse(it) }
        set(value) = prefs.edit().putString(KEY_VAULT_URI, value?.toString()).apply()

    var vaultName: String
        get() = prefs.getString(KEY_VAULT_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VAULT_NAME, value).apply()

    fun getWidgetTag(widgetId: Int): String {
        return prefs.getString(KEY_WIDGET_TAG_PREFIX + widgetId, "") ?: ""
    }

    fun setWidgetTag(widgetId: Int, tag: String) {
        prefs.edit().putString(KEY_WIDGET_TAG_PREFIX + widgetId, tag).apply()
    }

    fun removeWidgetTag(widgetId: Int) {
        prefs.edit().remove(KEY_WIDGET_TAG_PREFIX + widgetId).apply()
    }
}
