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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.asanaapp.data.storage.UserManager
import com.example.asanaapp.ui.components.AsanaButton
import com.example.asanaapp.ui.components.AsanaTextField
import com.example.asanaapp.ui.components.MessageDialog
import com.example.asanaapp.ui.components.ItemDropdown
import com.example.asanaapp.ui.components.isDateValid
import com.example.asanaapp.ui.components.isEmailValid
import com.example.asanaapp.ui.theme.InterFontFamily

/**
 * Screen to edit an existing task
 *
 * Responsibilities:
 *  - shows current task data (status, due date, assignee)
 *  - allows user to edit these values
 *  - validates assignee email and due date format
 *  - sends update to backend via [HomeViewModel.editTask]
 *  - shows loading / error / success feedback from [HomeViewModel.uiState]
 */
@Composable
fun EditTaskScreen(
    modifier: Modifier,
    taskId: String,
    assignee: String,
    role: String,
    existingDueDate: String,
    selected: String,
    navController: NavController,
    viewModel: HomeViewModel,
    userManager: UserManager
) {

    // Local state for form fields

    var dueDate by remember {
        mutableStateOf(existingDueDate) // prefill with existing due date
    }

    var status by remember {
        mutableStateOf(selected) // prefill with current status
    }

    var assigneeValue by remember {
        mutableStateOf(assignee) // prefill with current assignee
    }

    // ViewModel UI state (includes editTask loading/error/response)
    val ui = viewModel.uiState

    // Data from UserManager: statuses and currently selected project
    val statuses = userManager.statusesFlow.collectAsState(initial = null).value
    val currentProject = userManager.currentProjectFlow.collectAsState(initial = null).value

    // Whether we know that the current project was loaded
    var projectLoaded by remember {
        mutableStateOf(false)
    }

    // Validation flags
    var emailValid by remember {
        mutableStateOf(true)
    }

    var dateValid by remember {
        mutableStateOf(true)
    }

    // Dropdown options for status field
    var options by remember {
        mutableStateOf(listOf(""))
    }

    // Whether we know that the status options were loaded
    var optionsLoaded by remember {
        mutableStateOf(false)
    }

    /**
     * Mark project as loaded when currentProject becomes non-null
     */
    LaunchedEffect(currentProject) {
        if (currentProject != null) {
            projectLoaded = true
        }
    }

    /**
     * When statuses string loads (becomes non-null), split it into a list
     * and populate dropdown options
     */
    LaunchedEffect(statuses) {
        if (statuses != null) {
            options = statuses.split(",")
            optionsLoaded = true
        }
    }

    // -----------------------------
    // UI LAYOUT
    // -----------------------------

    Column(
        modifier = modifier
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // Top bar with back button
        Box(modifier = Modifier.fillMaxWidth()) {
            AsanaButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp),
                text = "←",
                paddingStart = 16,
                paddingEnd = 16,
                paddingBottom = 16,
                paddingTop = 32,
                fontSize = 18,
                onClickAction = { navController.navigate("tasks") }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            text = "Edit Task",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 46.sp
        )

        Spacer(Modifier.height(24.dp))

        // Due date field
        AsanaTextField(
            modifier = Modifier.fillMaxWidth(),
            text = "Due Date (YYYY-MM-DD)",
            fontSize = 18,
            paddingStart = 50,
            paddingEnd = 50,
            valueText = dueDate,
            onValueChange = { dueDate = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status dropdown (only shown once we have options from userManager)
        if (optionsLoaded) {
            ItemDropdown(
                options = options,
                onSelected = { status = it },
                selected = status,
                text = "Status"
            )
        }

        // Only managers can change the assignee
        if (role == "Manager") {
            Spacer(modifier = Modifier.height(12.dp))

            AsanaTextField(
                modifier = Modifier.fillMaxWidth(),
                text = "Assignee",
                fontSize = 18,
                paddingStart = 50,
                paddingEnd = 50,
                valueText = assigneeValue,
                onValueChange = { assigneeValue = it }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Submit button with validation + API call
        AsanaButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Submit",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 24,
            enabled = !ui.editTask.isLoading && projectLoaded, // disable while loading or project not loaded yet
            onClickAction = {

                // Validate email format for assignee
                if (isEmailValid(assigneeValue)) {
                    // Validate due date format
                    if (isDateValid(dueDate)) {
                        // Only call ViewModel if we have current project
                        if (currentProject != null) {
                            viewModel.editTask(
                                taskId = taskId,
                                dueDate = dueDate,
                                status = status,
                                projectId = currentProject,
                                assignee = assigneeValue
                            )
                        }
                    } else {
                        // Show invalid date dialog
                        dateValid = false
                    }
                } else {
                    // Show invalid email dialog
                    emailValid = false
                }
            }
        )

        // Local validation dialogs

        if (!emailValid) {
            MessageDialog(
                message = "Please enter a valid email address",
                onDismiss = { emailValid = true }
            )
        }

        if (!dateValid) {
            MessageDialog(
                message = "Please enter a valid due date",
                onDismiss = { dateValid = true }
            )
        }

        // Loading indicator while editTask request is running
        if (ui.editTask.isLoading) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }

    // ViewModel/backend error dialog
    ui.editTask.error?.let { msg ->
        MessageDialog(
            message = msg,
            onDismiss = { viewModel.clearError() }
        )
    }

    // ViewModel/backend success dialog
    ui.editTask.response?.let { resp ->
        MessageDialog(
            message = resp.msg,
            onDismiss = {
                viewModel.clearResponse()
                viewModel.consumeSuccess()
            }
        )
    }
}