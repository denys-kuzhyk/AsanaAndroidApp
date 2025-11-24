package com.example.asanaapp.ui.auth.signup

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
 * ViewModel responsible for handling sign up logic
 *
 * Responsibilities:
 *  - exposes [uiState] with loading/success/error/SignUpResponse
 *  - sends sign-up request to [AuthRepository]
 *  - updates UI state based on success/failure
 */
class SignUpViewModel(private val repository: AuthRepository) : ViewModel() {

    /**
     * Current UI state for the sign-up flow
     *
     * Contains:
     *  - [isLoading]: whether sign-up request is in progress
     *  - [success]:   flag used to trigger navigation on successful sign-up
     *  - [error]:     error message to show in dialog
     *  - [SignUpResponse]: full successful response from backend
     */
    var uiState by mutableStateOf(SignUpUiState())
        private set

    /**
     * Attempt to sign up with given [email] and [password]
     *
     * Flow:
     *  1. Set loading = true, clear previous error
     *  2. Call repository.signUp(...)
     *  3. On success -> set success = true and store [SignUpResponse]
     *  4. On failure -> set [error] message
     */
    fun signup(email: String, password: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val result = repository.signUp(AuthRequest(email, password))

            uiState = if (result.isSuccess) {
                uiState.copy(
                    isLoading = false,
                    success = true,
                    SignUpResponse = result.getOrNull()
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
     * Clear stored sign-up response from UI state
     * Call after showing any informational dialog based on it
     */
    fun clearSignUpResponse() {
        uiState = uiState.copy(SignUpResponse = null)
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
 * UI state for the sign-up screen
 */
data class SignUpUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val SignUpResponse: AuthResponseSuccess? = null
)