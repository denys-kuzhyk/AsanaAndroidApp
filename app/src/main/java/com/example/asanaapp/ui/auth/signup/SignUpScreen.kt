package com.example.asanaapp.ui.auth.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.asanaapp.ui.components.AsanaButton
import com.example.asanaapp.ui.components.AsanaTextField
import com.example.asanaapp.ui.components.MessageDialog
import com.example.asanaapp.ui.components.isEmailValid
import com.example.asanaapp.ui.theme.ButtonColor
import com.example.asanaapp.ui.theme.InterFontFamily

/**
 * First step of the sign-up flow: user provides their email
 *
 * Responsibilities:
 *  - Collects email input
 *  - Validates email format
 *  - Navigates to [PasswordScreen] when email is valid
 *  - Allows navigation back to the login screen
 */
@Composable
fun EmailScreen(
    modifier: Modifier,
    navController: NavController
) {

    // Email text field state
    var email by remember {
        mutableStateOf("")
    }

    // Flag that controls "invalid email" dialog visibility
    var emailValid by remember {
        mutableStateOf(true)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "New User",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Subtitle / instruction
        Text(
            text = "Please enter your email",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email input field
        AsanaTextField(
            modifier = Modifier.fillMaxWidth(),
            text = "Email",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 18,
            valueText = email,
            onValueChange = { email = it }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Next button: validate email and move to password screen
        AsanaButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Next",
            paddingStart = 100,
            paddingEnd = 100,
            fontSize = 24,
            onClickAction = {
                if (isEmailValid(email)) {
                    // pass email as route argument
                    navController.navigate("passwordScreen/$email")
                } else {
                    emailValid = false
                }
            }
        )

        // Invalid email dialog
        if (!emailValid) {
            MessageDialog(
                message = "Please enter a valid email address",
                onDismiss = { emailValid = true }
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        // Go back to login screen
        TextButton(
            onClick = { navController.navigate("login") },
            colors = ButtonColors(
                containerColor = Color.Transparent,
                contentColor = ButtonColor,
                disabledContainerColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            Text(
                text = "Have an account? Log In",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * Second step of the sign-up flow: user creates a password
 *
 * Responsibilities:
 *  - Collects and validates password + repeated password
 *    * passwords must match
 *    * minimum length of 10 characters
 *  - Triggers [SignUpViewModel.signup]
 *  - Shows loading / error / backend message
 *  - Navigates to homeScreen on successful sign up
 */
@Composable
fun PasswordScreen(
    modifier: Modifier,
    navController: NavController,
    email: String,
    viewModel: SignUpViewModel
) {

    // ViewModel UI state for sign-up
    val ui = viewModel.uiState

    // Password input states
    var password by remember {
        mutableStateOf("")
    }

    var passwordRepeat by remember {
        mutableStateOf("")
    }

    // Flags controlling validation dialogs
    var passwordMatch by remember {
        mutableStateOf(true)
    }

    var passwordLengthCorrect by remember {
        mutableStateOf(true)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        // Back button to email step
        AsanaButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            text = "←",
            paddingStart = 16,
            paddingEnd = 16,
            paddingBottom = 16,
            paddingTop = 32,
            fontSize = 18,
            onClickAction = { navController.navigate("emailScreen") }
        )

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Title
            Text(
                text = "Create a password",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            )

            Spacer(modifier = Modifier.height(35.dp))

            // Password field
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

            Spacer(modifier = Modifier.height(24.dp))

            // Repeat password field
            AsanaTextField(
                modifier = Modifier.fillMaxWidth(),
                text = "Repeat password",
                paddingStart = 50,
                paddingEnd = 50,
                fontSize = 18,
                password = true,
                valueText = passwordRepeat,
                onValueChange = { passwordRepeat = it }
            )

            Spacer(modifier = Modifier.height(35.dp))

            // Sign Up button:
            // - checks password equality
            // - checks length >= 10
            // - calls signup(email, password) when valid
            AsanaButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Sign Up",
                paddingStart = 100,
                paddingEnd = 100,
                fontSize = 24,
                onClickAction = {
                    if (password != passwordRepeat) {
                        passwordMatch = false
                    } else {
                        passwordLengthCorrect = password.length >= 10

                        if (passwordLengthCorrect) {
                            viewModel.signup(email, password)
                        }
                    }
                }
            )

            // Validation dialog: passwords mismatch
            if (!passwordMatch) {
                MessageDialog(
                    message = "The passwords do not match",
                    onDismiss = { passwordMatch = true }
                )
            }

            // Validation dialog: password too short
            if (!passwordLengthCorrect) {
                MessageDialog(
                    message = "Password must be at least 10 characters long",
                    onDismiss = { passwordLengthCorrect = true }
                )
            }

            // Loading spinner during signup request
            if (ui.isLoading) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }
        }
    }

    // Backend / ViewModel error dialog
    ui.error?.let { msg ->
        MessageDialog(
            message = msg,
            onDismiss = { viewModel.clearError() }
        )
    }

    // Backend / ViewModel response dialog (e.g. informational message)
    ui.SignUpResponse?.let { resp ->
        MessageDialog(
            message = resp.msg,
            onDismiss = { viewModel.clearSignUpResponse() }
        )
    }

    /**
     * One-shot navigation to homeScreen when signup succeeds
     * Back stack is cleared so the user cannot navigate back to auth flow
     */
    LaunchedEffect(ui.success) {
        if (ui.success) {

            navController.navigate("homeScreen") {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }

            viewModel.consumeSuccess()
        }
    }
}