package com.example.asanaapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.asanaapp.ui.components.AsanaButton
import com.example.asanaapp.ui.components.AsanaTextField
import com.example.asanaapp.ui.components.MessageDialog
import com.example.asanaapp.ui.theme.InterFontFamily

/**
 * Screen that lets the user change their password
 *
 * Flow:
 *  1. User enters current password, new password, repeat new password
 *  2. Local validation:
 *     - new password == repeated password
 *     - new password length >= 10
 *  3. On success -> calls [HomeViewModel.changePassword]
 *  4. Shows loading / error / success using [viewModel.uiState]
 */
@Composable
fun ChangePassword(
    modifier: Modifier,
    navController: NavController,
    viewModel: HomeViewModel
) {

    // Current password input
    var currentPassword by remember {
        mutableStateOf("")
    }

    // New password input
    var password by remember {
        mutableStateOf("")
    }

    // New password confirmation input
    var passwordRepeat by remember {
        mutableStateOf("")
    }

    // Flag to show "passwords don't match" dialog
    var passwordMatch by remember {
        mutableStateOf(true)
    }

    // Flag to show "password length" dialog
    var passwordLengthCorrect by remember {
        mutableStateOf(true)
    }

    // UI state from HomeViewModel (contains loading/error/response for changePassword)
    val ui = viewModel.uiState

    Box(
        modifier = modifier
    ) {

        // Back button to Account screen
        AsanaButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(36.dp),
            text = "←",
            paddingStart = 16,
            paddingEnd = 16,
            paddingBottom = 16,
            paddingTop = 32,
            fontSize = 18,
            onClickAction = { navController.navigate("account") }
        )

        // Main content column
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Title
            Text(
                "Change Password",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            )

            Spacer(modifier = Modifier.height(46.dp))

            // Current password field
            AsanaTextField(
                modifier = Modifier.fillMaxWidth(),
                text = "Current Password",
                fontSize = 18,
                paddingStart = 50,
                paddingEnd = 50,
                valueText = currentPassword,
                onValueChange = { currentPassword = it },
                password = true      // hides characters
            )

            Spacer(modifier = Modifier.height(12.dp))

            // New password field
            AsanaTextField(
                modifier = Modifier.fillMaxWidth(),
                text = "New Password",
                fontSize = 18,
                paddingStart = 50,
                paddingEnd = 50,
                valueText = password,
                onValueChange = { password = it },
                password = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Repeat new password field
            AsanaTextField(
                modifier = Modifier.fillMaxWidth(),
                text = "Repeat New Password",
                fontSize = 18,
                paddingStart = 50,
                paddingEnd = 50,
                valueText = passwordRepeat,
                onValueChange = { passwordRepeat = it },
                password = true
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Submit button: runs simple client-side validation, then calls ViewModel
            AsanaButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Submit",
                paddingStart = 50,
                paddingEnd = 50,
                fontSize = 24,
                onClickAction = {
                    if (password != passwordRepeat) {
                        // Show "passwords don't match" dialog
                        passwordMatch = false
                    } else {
                        // Validate password length
                        passwordLengthCorrect = password.length >= 10

                        // If valid, trigger ViewModel changePassword call
                        if (passwordLengthCorrect) {
                            viewModel.changePassword(currentPassword, password)
                        }
                    }
                }
            )

            // Local validation error: passwords don't match
            if (!passwordMatch) {
                MessageDialog(
                    message = "The passwords do not match",
                    onDismiss = { passwordMatch = true }
                )
            }

            // Local validation error: password length too short
            if (!passwordLengthCorrect) {
                MessageDialog(
                    message = "Password must be at least 10 characters long",
                    onDismiss = { passwordLengthCorrect = true }
                )
            }

            // Show loading spinner while changePassword request is in progress
            if (ui.changePassword.isLoading) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }
        }

        // Backend / ViewModel error
        ui.changePassword.error?.let { msg ->
            MessageDialog(
                message = msg,
                onDismiss = { viewModel.clearError() }
            )
        }

        // Backend / ViewModel success response
        ui.changePassword.response?.let { resp ->
            MessageDialog(
                message = resp.msg,
                onDismiss = {
                    viewModel.clearResponse()
                    viewModel.consumeSuccess()
                }
            )
        }
    }
}