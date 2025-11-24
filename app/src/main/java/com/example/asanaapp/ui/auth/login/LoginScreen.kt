package com.example.asanaapp.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.asanaapp.ui.components.AsanaButton
import com.example.asanaapp.ui.components.AsanaTextField
import com.example.asanaapp.ui.components.MessageDialog
import com.example.asanaapp.ui.theme.InterFontFamily

/**
 * Login screen.
 *
 * Responsibilities:
 *  - Collects user credentials (email + password)
 *  - Triggers login via [LoginViewModel.login]
 *  - Shows loading spinner while logging in
 *  - Displays error / backend message dialogs
 *  - Navigates to homeScreen on successful login
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: LoginViewModel
) {
    // UI state from ViewModel (loading, success, error, responses)
    val ui = viewModel.uiState

    // Local text field states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Guard to make sure we only navigate once per successful login
    var navigated by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Screen title
        Text(
            "Log In",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp
        )

        Spacer(Modifier.height(50.dp))

        // Email input
        AsanaTextField(
            modifier = Modifier.fillMaxWidth(),
            text = "Email",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 18,
            valueText = email,
            onValueChange = { email = it }
        )

        Spacer(Modifier.height(30.dp))

        // Password input (masked)
        AsanaTextField(
            modifier = Modifier.fillMaxWidth(),
            text = "Password",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 18,
            password = true,
            valueText = password,
            onValueChange = { password = it }
        )

        Spacer(Modifier.height(40.dp))

        // Log in button
        AsanaButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Log in",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 24,
            onClickAction = {
                // Prevent spamming login while loading or after navigation
                if (!ui.isLoading && !navigated) {
                    viewModel.login(email, password)
                }
            }
        )

        Spacer(Modifier.height(50.dp))

        // Navigate to Sign Up flow
        TextButton(
            onClick = {
                if (!navigated) {
                    navController.navigate("emailScreen")
                }
            }
        ) {
            Text(
                "Sign Up",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // Show loading indicator during login request
        if (ui.isLoading) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }

    // Error dialog (validation / backend error)
    ui.error?.let { msg ->
        MessageDialog(
            message = msg,
            onDismiss = { viewModel.clearError() }
        )
    }

    // Backend login response message (e.g. success msg from API)
    ui.loginResponse?.let { resp ->
        MessageDialog(
            message = resp.msg,
            onDismiss = { viewModel.clearLoginResponse() }
        )
    }

    /**
     * Observe success flag and navigate exactly once when login succeeds:
     *  - clear response + success flag in ViewModel
     *  - clear back stack and go to homeScreen
     */
    LaunchedEffect(ui.success) {
        if (ui.success && !navigated) {
            navigated = true
            viewModel.clearLoginResponse()
            viewModel.consumeSuccess()

            navController.navigate("homeScreen") {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
}