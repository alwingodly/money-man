package com.alwin.moneymanager.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alwin.moneymanager.util.downsampleImageToJpeg
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val PROFILE_NAME_KEY = stringPreferencesKey("profile_name")
private val PROFILE_PHOTO_PATH_KEY = stringPreferencesKey("profile_photo_path")

/** Long enough edge for both the small avatar and the full-screen photo viewer to look sharp,
 * short enough that the stored file stays a few tens of KB instead of several MB. */
private const val PROFILE_PHOTO_MAX_DIMENSION = 720

@Singleton
class ProfileRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) {
    val name: Flow<String?> = dataStore.data.map { prefs -> prefs[PROFILE_NAME_KEY] }

    val photoPath: Flow<String?> = dataStore.data.map { prefs -> prefs[PROFILE_PHOTO_PATH_KEY] }

    suspend fun setName(name: String) {
        dataStore.edit { prefs -> prefs[PROFILE_NAME_KEY] = name }
    }

    /** Downsamples the picked image and copies it into app-internal storage so it survives
     * independent of the source URI's grant (gallery permissions, reboots, the source app
     * uninstalling, etc) — see [PROFILE_PHOTO_MAX_DIMENSION] for why it's downsampled rather
     * than copied as-is. */
    suspend fun setPhoto(uri: Uri) {
        val file = withContext(Dispatchers.IO) {
            val target = File(context.filesDir, "profile_photo.jpg")
            downsampleImageToJpeg(
                resolver = context.contentResolver,
                source = uri,
                destination = target,
                maxDimension = PROFILE_PHOTO_MAX_DIMENSION,
            )
            target
        }
        dataStore.edit { prefs -> prefs[PROFILE_PHOTO_PATH_KEY] = file.absolutePath }
    }
}
