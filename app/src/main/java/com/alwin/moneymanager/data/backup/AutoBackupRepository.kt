package com.alwin.moneymanager.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val ENABLED = booleanPreferencesKey("auto_backup_enabled")
private val TREE_URI = stringPreferencesKey("auto_backup_tree_uri")
private val FILE_URI = stringPreferencesKey("auto_backup_file_uri")

private const val BACKUP_FOLDER_NAME = "MoneyManagerBackups"
const val AUTO_BACKUP_FILE_NAME = "moneymanager-auto-backup.json"

private val URI_PERMISSION_FLAGS =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

/**
 * Remembers the folder the user picked for automatic JSON snapshots. Files live in a
 * `MoneyManagerBackups` subfolder of that tree so they survive an uninstall (unlike Room, which
 * lives in private app storage and is wiped with the package).
 */
@Singleton
class AutoBackupRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) {
    val isEnabled: Flow<Boolean> = dataStore.data.map { it[ENABLED] == true }

    suspend fun fileUriOrNull(): Uri? {
        val prefs = dataStore.data.first()
        if (prefs[ENABLED] != true) return null
        return prefs[FILE_URI]?.let(Uri::parse)
    }

    suspend fun enable(treeUri: Uri): Uri {
        context.contentResolver.takePersistableUriPermission(treeUri, URI_PERMISSION_FLAGS)
        val fileUri = createOrFindBackupFile(treeUri)
        val previousTree = dataStore.data.first()[TREE_URI]
        dataStore.edit { prefs ->
            prefs[ENABLED] = true
            prefs[TREE_URI] = treeUri.toString()
            prefs[FILE_URI] = fileUri.toString()
        }
        if (previousTree != null && previousTree != treeUri.toString()) {
            releasePersistedPermission(Uri.parse(previousTree))
        }
        return fileUri
    }

    suspend fun disable() {
        val previousTree = dataStore.data.first()[TREE_URI]
        dataStore.edit { prefs ->
            prefs[ENABLED] = false
            prefs.remove(TREE_URI)
            prefs.remove(FILE_URI)
        }
        if (previousTree != null) releasePersistedPermission(Uri.parse(previousTree))
    }

    /**
     * If the remembered file was deleted, recreate it in the saved tree. Returns null when auto
     * backup is off or the folder permission is gone (in which case auto backup is turned off).
     */
    suspend fun recreateFile(): Uri? {
        val prefs = dataStore.data.first()
        if (prefs[ENABLED] != true) return null
        val tree = prefs[TREE_URI]?.let(Uri::parse) ?: return null
        return try {
            val fileUri = createOrFindBackupFile(tree)
            dataStore.edit { it[FILE_URI] = fileUri.toString() }
            fileUri
        } catch (_: Exception) {
            disable()
            null
        }
    }

    private fun createOrFindBackupFile(treeUri: Uri): Uri {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Could not open the selected folder")
        val folder = root.findFile(BACKUP_FOLDER_NAME)?.takeIf { it.isDirectory }
            ?: root.createDirectory(BACKUP_FOLDER_NAME)
            ?: throw IOException("Could not create the backup folder")
        val file = folder.findFile(AUTO_BACKUP_FILE_NAME)
            ?: folder.createFile("application/json", AUTO_BACKUP_FILE_NAME)
            ?: throw IOException("Could not create the backup file")
        return file.uri
    }

    private fun releasePersistedPermission(uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(uri, URI_PERMISSION_FLAGS)
        } catch (_: SecurityException) {
            // Already released or never granted on this device.
        }
    }
}
