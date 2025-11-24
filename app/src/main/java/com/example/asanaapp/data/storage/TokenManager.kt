package com.example.asanaapp.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

/**
 * Secure storage for authentication tokens (access + refresh)
 *
 * Uses AndroidX Security:
 *  - [MasterKey] for generating/storing a strong key in the system keystore
 *  - [EncryptedSharedPreferences] for encrypting both keys and values at rest
 *
 * This ensures tokens are not stored in plain text on the device
 */
class TokenManager(context: Context) {


    // Master key stored in Android Keystore with AES-256-GCM encryption scheme
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /**
     * Encrypted SharedPreferences instance
     *
     * - "auth_prefs" is the file name
     * - AES256_SIV for key (preference name) encryption
     * - AES256_GCM for value encryption
     */
    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save access and refresh tokens securely
     */
    fun saveTokens(access: String?, refresh: String?) {
        sharedPrefs.edit().apply {
            putString("access_token", access)
            putString("refresh_token", refresh)
            apply()
        }
    }

    /**
     * Retrieve stored access token, or null if not present
     */
    fun getAccessToken(): String? = sharedPrefs.getString("access_token", null)

    /**
     * Retrieve stored refresh token, or null if not present
     */
    fun getRefreshToken(): String? = sharedPrefs.getString("refresh_token", null)

    /**
     * Clear all stored authentication data (both tokens)
     */
    fun clear() {
        sharedPrefs.edit { clear() }
    }
}