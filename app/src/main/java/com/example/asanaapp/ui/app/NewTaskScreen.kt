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
import com.example.asanaapp.ui.components.ItemDropdown
import com.example.asanaapp.ui.components.MessageDialog
import com.example.asanaapp.ui.components.isDateValid
import com.example.asanaapp.ui.components.isEmailValid
import com.example.asanaapp.ui.theme.InterFontFamily

/**
 * Screen for creating a new task
 *
 * Responsibilities:
 *  - collects task data from user (name, description, due date, assignee, status)
 *  - validates email and due date format
 *  - uses current project from [UserManager]
 *  - calls [HomeViewModel.createTask] and reacts to loading / error / success
 */
@Composable
fun NewTaskScreen(
    modifier: Modifier,
    navController: NavController,
    viewModel: HomeViewModel,
    userManager: UserManager
) {

    // Current project ID and available statuses from DataStore
    val currentProject = userManager.currentProjectFlow.collectAsState(initial = null).value
    val statuses = userManager.statusesFlow.collectAsState(initial = null).value

    // Form fields
    var dueDate by remember { mutableStateOf("") }
    var taskName by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf("") }

    // Flags controlling if current project and statuses list are ready
    var projectLoaded by remember { mutableStateOf(false) }

    var options by remember {
        mutableStateOf(listOf(""))
    }

    var optionsLoaded by remember {
        mutableStateOf(false)
    }

    // Currently selected status (default to first option when loaded)
    var currentStatus by remember {
        mutableStateOf(options[0])
    }

    // Validation flags
    var emailValid by remember { mutableStateOf(true) }
    var dateValid by remember { mutableStateOf(true) }

    // Max length for task name
    val nameMaxLength = 18

    // Full UI state from ViewModel (we only use createTask part here)
    val ui = viewModel.uiState

    /**
     * Set projectLoaded once currentProject is non-null
     */
    LaunchedEffect(currentProject) {
        if (currentProject != null) {
            projectLoaded = true
        }
    }

    /**
     * When statuses are loaded, split CSV string into list
     * and enable dropdown
     */
    LaunchedEffect(statuses) {
        if (statuses != null) {
            options = statuses.split(",")
            optionsLoaded = true
            // Reset currentStatus if needed
            if (options.isNotEmpty()) {
                currentStatus = options[0]
            }
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

        // Back button to Tasks screen
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
            text = "New Task",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 46.sp
        )

        Spacer(Modifier.height(24.dp))

        // Task name (with max length restriction)
        AsanaTextField(
            modifier = Modifier.fillMaxWidth(),
            text = "Name",
            fontSize = 18,
            paddingStart = 50,
            paddingEnd = 50,
            valueText = taskName,
            onValueChange = {
                if (it.length <= nameMaxLength) {
                    taskName = it
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        // Task description (multiline)
        AsanaTextField(
            modifier = Modifier.fillMaxWidth(),
            text = "Description",
            fontSize = 18,
            paddingStart = 50,
            paddingEnd = 50,
            valueText = taskDescription,
            onValueChange = { taskDescription = it },
            expanded = true
        )

        Spacer(Modifier.height(12.dp))

        // Due date input
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

        // Assignee email input
        AsanaTextField(
            modifier = Modifier.fillMaxWidth(),
            text = "Assignee",
            fontSize = 18,
            paddingStart = 50,
            paddingEnd = 50,
            valueText = assignee,
            onValueChange = { assignee = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status dropdown (once options are loaded)
        if (optionsLoaded) {
            ItemDropdown(
                options = options,
                onSelected = { currentStatus = it },
                selected = currentStatus,
                text = "Status"
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Submit button with validation and API call
        AsanaButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Submit",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 24,
            enabled = !ui.createTask.isLoading && projectLoaded,
            onClickAction = {

                // 1) Validate assignee email
                if (isEmailValid(assignee)) {
                    // 2) Validate due date format
                    if (isDateValid(dueDate)) {
                        // 3) If project is available, call ViewModel
                        if (currentProject != null) {
                            viewModel.createTask(
                                dueDate = dueDate,
                                status = currentStatus,
                                projectId = currentProject,
                                name = taskName,
                                description = taskDescription,
                                assignee = assignee
                            )
                        }
                    } else {
                        dateValid = false
                    }
                } else {
                    emailValid = false
                }
            }
        )

        // Local validation dialogs

        if (!dateValid) {
            MessageDialog(
                message = "Please enter a valid due date",
                onDismiss = { dateValid = true }
            )
        }

        if (!emailValid) {
            MessageDialog(
                message = "Please enter a valid email address",
                onDismiss = { emailValid = true }
            )
        }

        // Show loading indicator while createTask request is running
        if (ui.createTask.isLoading) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }

    // Backend/ViewModel error dialog
    ui.createTask.error?.let { msg ->
        MessageDialog(
            message = msg,
            onDismiss = { viewModel.clearError() }
        )
    }

    // Backend/ViewModel success dialog
    ui.createTask.response?.let { resp ->
        MessageDialog(
            message = resp.msg,
            onDismiss = {
                viewModel.clearResponse()
                viewModel.consumeSuccess()

                // Reset form after successful creation
                taskName = ""
                taskDescription = ""
                dueDate = ""
                assignee = ""
                currentStatus = options[0]
            }
        )
    }
}