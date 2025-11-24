package com.example.asanaapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.asanaapp.data.model.TasksResponse
import com.example.asanaapp.data.storage.UserManager
import com.example.asanaapp.ui.components.AsanaButton
import com.example.asanaapp.ui.components.MessageDialog
import com.example.asanaapp.ui.components.TaskItem
import com.example.asanaapp.ui.theme.InterFontFamily

/**
 * Screen displaying the list of tasks for the current project
 *
 * Responsibilities:
 *  - loads tasks from backend via [HomeViewModel.getTasks]
 *  - handles token expiration by calling [HomeViewModel.refreshBlocking]
 *  - shows a list of tasks using [TaskItem]
 *  - allows Manager to create new tasks and delete existing ones
 *  - shows loading / error / success feedback for delete operation
 */
@Composable
fun TasksScreen(
    modifier: Modifier,
    navController: NavController,
    viewModel: HomeViewModel,
    userManager: UserManager
) {

    // Role and current project of the user from DataStore
    val role = userManager.roleFlow.collectAsState(initial = null).value
    val currentProject = userManager.currentProjectFlow.collectAsState(initial = null).value

    // LazyColumn scroll state
    val listState = rememberLazyListState()

    // Local copy of tasks response
    var tasks by remember {
        mutableStateOf(
            TasksResponse(
                msg = "",
                user_tasks = listOf(),
                statuses = ""
            )
        )
    }

    // ViewModel UI state for tasks and delete operations
    val uiState = viewModel.uiState
    val error = uiState.tasks.error

    // Used to ensure initial getTasks is only called once
    var ranOnce by rememberSaveable { mutableStateOf(false) }

    /**
     * Initial tasks load:
     *  - only when we have both role and currentProject
     *  - only run once per composition
     */
    LaunchedEffect(role, currentProject) {
        if (!ranOnce && role != null && currentProject != null) {
            ranOnce = true
            viewModel.getTasks(role = role, projectId = currentProject)
        }
    }

    /**
     * Handle token expiration for tasks loading
     *
     * If error indicates expired/invalid token:
     *  - call refreshBlocking()
     *  - retry getTasks() on success
     *  - logout + navigate to login on failure
     */
    LaunchedEffect(error) {
        if (error == "Token has expired" || error == "Token is not valid anymore") {
            val refreshed = viewModel.refreshBlocking()
            if (refreshed.isSuccess) {
                if (role != null && currentProject != null) {
                    viewModel.getTasks(role = role, projectId = currentProject)
                } // retry after successful refresh
            } else {
                // Refresh failed -> logout and clear back stack, go to login
                viewModel.logout()
                navController.navigate("login") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                        saveState = false
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
            // Prevent re-trigger on recompositions
            viewModel.clearError()
        }
    }

    // Show any non-auth-related error in a dialog
    if (error != null && error != "Token has expired" && error != "Token is not valid anymore") {
        MessageDialog(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }

    // Consume generic "success" flag once read
    LaunchedEffect(uiState.tasks.success) {
        viewModel.consumeSuccess()
    }

    /**
     * When tasks response changes, update local [tasks] variable
     */
    LaunchedEffect(uiState.tasks.response) {
        uiState.tasks.response?.let {
            tasks = it
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

        // Top bar with back button + "New Task" for Manager
        Box(modifier = Modifier.fillMaxWidth()) {
            // Back to Home
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
                onClickAction = { navController.navigate("homeScreen") }
            )

            // "New Task" only visible for Manager role
            if (role == "Manager") {
                AsanaButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 24.dp),
                    text = "New Task",
                    paddingStart = 16,
                    paddingEnd = 16,
                    paddingBottom = 16,
                    paddingTop = 32,
                    fontSize = 18,
                    onClickAction = { navController.navigate("newTask") }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Title
        Text(
            text = "Tasks",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 46.sp
        )

        Spacer(Modifier.height(24.dp))

        // Task list
        LazyColumn(
            state = listState,
            modifier = modifier
        ) {
            itemsIndexed(tasks.user_tasks) { _, task ->

                TaskItem(
                    name = task.name,
                    role = role,
                    assignee = task.assigneeId,
                    description = task.description,
                    status = task.status,
                    dueDate = task.dueDate,
                    onClickEdit = {
                        navController.navigate(
                            "editTasks/${task.status}/${task.dueDate}/${task.taskId}/${task.assigneeId}/$role"
                        )
                    },
                    onClickDelete = {
                        if (role != null && currentProject != null) {
                            viewModel.onDeleteTaskClicked(
                                taskId = task.taskId,
                                role = role,
                                projectId = currentProject
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Show loading indicator while deleteTask is in progress
        if (uiState.deleteTask.isLoading) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }

    // Delete operation error dialog
    uiState.deleteTask.error?.let { msg ->
        MessageDialog(
            message = msg,
            onDismiss = { viewModel.clearError() }
        )
    }

    // Delete operation success dialog
    uiState.deleteTask.response?.let { resp ->
        MessageDialog(
            message = resp.msg,
            onDismiss = {
                viewModel.clearResponse()
                viewModel.consumeSuccess()
            }
        )
    }
}