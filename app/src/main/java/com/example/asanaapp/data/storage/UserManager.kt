package com.example.asanaapp.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map



// Helpers for Map< String, String >
private val gson = Gson()
private val stringMapType = object : TypeToken<Map<String, String>>() {}.type

/**
 * Extension to convert Map<String, String> to JSON string
 */
private fun Map<String, String>.toJson(): String =
    gson.toJson(this, stringMapType)

/**
 * Extension to parse JSON string into Map<String, String>
 */
private fun String?.toStringMap(): Map<String, String> =
    if (this.isNullOrEmpty()) emptyMap()
    else gson.fromJson(this, stringMapType)


// ---------------------------
// DataStore setup
// ---------------------------

/**
 * Single DataStore instance name for user-related preferences
 */
private const val USER_DS_NAME = "user_prefs"

/**
 * Extension property that exposes a DataStore<Preferences>
 * tied to the given [Context]
 */
val Context.userDataStore by preferencesDataStore(name = USER_DS_NAME)

/**
 * Keys used inside DataStore for storing user properties.
 */
private object UserKeys {
    val ID                = stringPreferencesKey("user_id")
    val NAME              = stringPreferencesKey("user_name")
    val EMAIL             = stringPreferencesKey("user_email")
    val ROLE              = stringPreferencesKey("user_role")
    val CURRENT_PROJECT   = stringPreferencesKey("current_project")
    val STATUSES          = stringPreferencesKey("statuses")
    val PROJECTS_MAP_JSON = stringPreferencesKey("projects_map_json")
}

/**
 * In-memory representation of a user profile stored in DataStore
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val currentProject: String,
    val statuses: String = "",
    val projectsMap: Map<String, String> = emptyMap()
)

/**
 * UserManager wraps DataStore and provides:
 *  - typed flows for observing user data
 *  - helper methods to save/update/clear user data
 */
class UserManager(private val context: Context) {


    // Individual flows for reading specific user fields

    val emailFlow:   Flow<String?> = context.userDataStore.data.map { it[UserKeys.EMAIL] }
    val nameFlow:    Flow<String?> = context.userDataStore.data.map { it[UserKeys.NAME] }
    val idFlow:      Flow<String?> = context.userDataStore.data.map { it[UserKeys.ID] }
    val roleFlow:    Flow<String?> = context.userDataStore.data.map { it[UserKeys.ROLE] }
    val currentProjectFlow: Flow<String?> = context.userDataStore.data.map { it[UserKeys.CURRENT_PROJECT] }
    val statusesFlow:       Flow<String?> = context.userDataStore.data.map { it[UserKeys.STATUSES] }

    /**
     * Flow that emits the projects map as Map<String, String>.
     * Internally stored as JSON string in DataStore.
     */
    val projectsMapFlow: Flow<Map<String, String>> =
        context.userDataStore.data.map { it[UserKeys.PROJECTS_MAP_JSON].toStringMap() }

    /**
     * Save or update user data in DataStore
     *
     * Any nullable argument that is null will be ignored (keeps old value)
     * - `statuses` defaults to empty string if not provided
     * - `projectsMap` is stored as JSON
     */
    suspend fun saveUser(
        id: String?,
        name: String?,
        email: String?,
        role: String?,
        currentProject: String?,
        statuses: String = "",
        projectsMap: Map<String, String>?
    ) {
        context.userDataStore.edit { prefs ->
            if (id != null)              prefs[UserKeys.ID] = id
            if (name != null)            prefs[UserKeys.NAME] = name
            if (email != null)           prefs[UserKeys.EMAIL] = email
            if (role != null)            prefs[UserKeys.ROLE] = role
            if (currentProject != null)  prefs[UserKeys.CURRENT_PROJECT] = currentProject
            // statuses is always written (even if empty string)
            prefs[UserKeys.STATUSES] = statuses
            if (projectsMap != null) {
                prefs[UserKeys.PROJECTS_MAP_JSON] = projectsMap.toJson()
            }
        }
    }

    /**
     * Update only currently selected project id
     */
    suspend fun updateCurrentProject(projectId: String) {
        context.userDataStore.edit { prefs ->
            prefs[UserKeys.CURRENT_PROJECT] = projectId
        }
    }

    /**
     * Update only statuses string
     * (e.g. after a fresh getTasks() call)
     */
    suspend fun updateStatuses(statuses: String) {
        context.userDataStore.edit { prefs ->
            prefs[UserKeys.STATUSES] = statuses
        }
    }

    /**
     * Clear all stored user-related data
     */
    suspend fun clear() {
        context.userDataStore.edit { prefs ->
            prefs.remove(UserKeys.ID)
            prefs.remove(UserKeys.NAME)
            prefs.remove(UserKeys.EMAIL)
            prefs.remove(UserKeys.ROLE)
            prefs.remove(UserKeys.CURRENT_PROJECT)
            prefs.remove(UserKeys.STATUSES)
            prefs.remove(UserKeys.PROJECTS_MAP_JSON)
        }
    }
}