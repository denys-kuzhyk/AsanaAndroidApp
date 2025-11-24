package com.example.asanaapp.ui.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asanaapp.data.model.AuthRequest
import com.example.asanaapp.data.model.AuthResponseSuccess
import com.example.asanaapp.data.network.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for handling login logic
 *
 * Responsibilities:
 *  - exposes [uiState] with loading/success/error/loginResponse
 *  - sends login request to [AuthRepository]
 *  - updates UI state based on success/failure
 */
class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    /**
     * Current UI state for the login screen
     *
     * Contains:
     *  - [isLoading]: whether login request is in progress
     *  - [success]:    flag used to trigger navigation on successful login
     *  - [error]:      error message to show in dialog
     *  - [loginResponse]: full successful response from backend
     */
    var uiState by mutableStateOf(LoginUiState())
        private set

    /**
     * Attempt to log in with given [email] and [password]
     *
     * Flow:
     *  1. Set loading = true, clear previous error
     *  2. Call repository.login(...)
     *  3. On success -> set success = true and store [loginResponse]
     *  4. On failure -> set [error] message
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = repository.login(AuthRequest(email, password))

            uiState = if (result.isSuccess) {
                uiState.copy(
                    isLoading = false,
                    success = true,
                    loginResponse = result.getOrNull()
                )
            } else {
                uiState.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    /**
     * Clear current error message from UI state
     * Call after dismissing error dialog
     */
    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    /**
     * Clear stored login response from UI state.
     * Call after showing any informational dialog based on it
     */
    fun clearLoginResponse() {
        uiState = uiState.copy(loginResponse = null)
    }

    /**
     * Reset success flag so navigation or other "one-shot" effects
     * are not triggered multiple times
     */
    fun consumeSuccess() {
        uiState = uiState.copy(success = false)
    }
}

/**
 * UI state for the login screen
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val loginResponse: AuthResponseSuccess? = null
)