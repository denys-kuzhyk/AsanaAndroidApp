package com.example.asanaapp.data.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple authentication state manager
 *
 * Responsibilities:
 *  - keeps track of whether the user is currently logged in
 *  - exposes this information as a [StateFlow] so UI can react to changes
 *  - uses [TokenManager] as the single source of truth (if we have a token -> logged in)
 */
class AuthManager(private val tokenManager: TokenManager) {

    /**
     * Backing mutable flow for login state
     *
     * Initial value is based on whether we currently have a non-empty access token
     */
    private val _isLoggedIn = MutableStateFlow(checkLoggedIn())

    /**
     * Public read-only view of the login state
     */
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    /**
     * Checks if the user should be considered logged in.
     */
    private fun checkLoggedIn(): Boolean = !tokenManager.getAccessToken().isNullOrBlank()

    /**
     * Log user out:
     *  - clear all tokens
     *  - update login state to false
     */
    fun setLoggedOut() {
        tokenManager.clear()
        _isLoggedIn.value = false
    }

    /**
     * Mark user as logged in
     *
     * Token persisting is handled in [TokenManager]
     * Here we only update the in-memory login state
     */
    fun setLoggedIn() {
        _isLoggedIn.value = true
    }
}