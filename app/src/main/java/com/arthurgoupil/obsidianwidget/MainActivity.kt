package com.arthurgoupil.obsidianwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesHelper
    private lateinit var folderPathText: TextView
    private lateinit var vaultNameEdit: EditText

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            prefs.vaultUri = it
            updateFolderDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PreferencesHelper(this)
        folderPathText = findViewById(R.id.folder_path_text)
        vaultNameEdit = findViewById(R.id.vault_name_edit)

        // Check if stored URI permission was revoked (e.g. after reinstall)
        val storedUri = prefs.vaultUri
        if (storedUri != null && !hasUriPermission(storedUri)) {
            prefs.vaultUri = null
            Toast.makeText(this, getString(R.string.permission_revoked), Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.select_folder_button).setOnClickListener {
            folderPicker.launch(null)
        }

        findViewById<Button>(R.id.save_button).setOnClickListener {
            prefs.vaultName = vaultNameEdit.text.toString().trim()
            finish()
        }

        updateFolderDisplay()
        vaultNameEdit.setText(prefs.vaultName)
    }

    private fun hasUriPermission(uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    }

    private fun updateFolderDisplay() {
        val uri = prefs.vaultUri
        folderPathText.text = if (uri != null) {
            uri.lastPathSegment ?: uri.toString()
        } else {
            getString(R.string.no_folder_selected)
        }
    }
}

