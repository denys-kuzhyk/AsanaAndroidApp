package com.example.asanaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.asanaapp.data.network.AuthRepository
import com.example.asanaapp.data.storage.AuthManager
import com.example.asanaapp.data.storage.TokenManager
import com.example.asanaapp.data.storage.UserManager
import com.example.asanaapp.navigation.AppNavigation
import com.example.asanaapp.ui.app.HomeViewModel
import com.example.asanaapp.ui.auth.login.LoginViewModel
import com.example.asanaapp.ui.auth.signup.SignUpViewModel
import com.example.asanaapp.ui.theme.AsanaAppTheme

/**
 * Main entry point of the app
 *
 * Responsibilities:
 *  - Initialize core singletons (TokenManager, AuthManager, UserManager, AuthRepository)
 *  - Create ViewModels (Login, SignUp, Home) and pass them into the Compose tree
 *  - Set up the root navigation graph via [AppNavigation]
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- Core data / auth layer singletons ---

        // Handles secure storage of access & refresh tokens
        val tokenManager = TokenManager(context = this)

        // Tracks whether user is logged in, based on tokens
        val authManager = AuthManager(tokenManager)

        // DataStore-based storage for user profile, current project, statuses, etc.
        val userManager = UserManager(context = this)

        // Repository that talks to the backend API and coordinates auth + user storage
        val repository = AuthRepository(tokenManager, authManager, userManager)

        // --- ViewModels used across the app ---

        // ViewModel for login screen
        val loginViewModel = LoginViewModel(repository)

        // ViewModel for sign up flow
        val signUpViewModel = SignUpViewModel(repository)

        // ViewModel for "app" area (home, tasks, edit/create, change password)
        val homeViewModel = HomeViewModel(repository, userManager)

        setContent {
            AsanaAppTheme {

                // Root navigation host, decides whether to show login or home
                AppNavigation(
                    modifier = Modifier.fillMaxSize(),
                    loginViewModel = loginViewModel,
                    signUpViewModel = signUpViewModel,
                    homeViewModel = homeViewModel,
                    authManager = authManager,
                    userManager = userManager
                )
            }
        }
    }
}