package com.example.asanaapp.ui.app

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.asanaapp.ui.components.ItemDropdown
import com.example.asanaapp.ui.components.MessageDialog
import com.example.asanaapp.ui.theme.InterFontFamily
import kotlinx.coroutines.launch

/**
 * Main Home screen of the app
 *
 * Responsibilities:
 *  - Shows project selector (for Manager role)
 *  - Shows navigation buttons to Account and Tasks screens
 *  - Displays basic tasks statistics: open / completed / total
 *  - Triggers task loading based on current project and role
 *  - Handles token expiration (refresh + retry / logout)
 */
@Composable
fun Home(
    modifier: Modifier,
    navController: NavController,
    userManager: UserManager,
    viewModel: HomeViewModel
) {

    // Map with "open"/"completed"/"total" task counts
    var taskStatistic by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Scope for launching coroutines (used for logout)
    val scope = rememberCoroutineScope()

    // Local copy of TasksResponse used to compute statistics
    var tasks by remember {
        mutableStateOf(
            TasksResponse(
                msg = "",
                user_tasks = listOf(),
                statuses = ""
            )
        )
    }

    // User-related info from DataStore
    val role = userManager.roleFlow.collectAsState(initial = null).value
    val projectsMap = userManager.projectsMapFlow.collectAsState(initial = null).value
    val currentProject = userManager.currentProjectFlow.collectAsState(initial = null).value

    // Currently selected project name in dropdown
    var selected by remember {
        mutableStateOf("Select a project")
    }

    // List of project names for dropdown
    val projectNames = remember {
        mutableStateListOf<String>()
    }

    // Map of projectId -> projectName (reverse of projectsMap which is name -> id)
    val projectIds = remember {
        mutableStateMapOf<String, String>()
    }

    // UI state from ViewModel (for tasks, errors, etc.)
    val uiState = viewModel.uiState
    val error = uiState.tasks.error

    // Used to ensure the initial "load tasks for current project" runs only once
    var ranOnce by rememberSaveable { mutableStateOf(false) }

    /**
     * Populate dropdown data (projectNames, projectIds) whenever projectsMap changes
     *
     * projectsMap: Map<projectName, projectId> saved in UserManager
     */
    LaunchedEffect(projectsMap) {
        if (projectsMap != null) {
            for (name in projectsMap.keys) {
                projectNames.add(name)
            }
            for ((name, id) in projectsMap) {
                // Reverse mapping: projectId -> projectName
                projectIds[id] = name
            }
        }
    }

    /**
     * Initial task loading:
     *  - wait until role, currentProject and projectsMap are non-null / not empty
     *  - set selected project in dropdown based on currentProject
     *  - trigger initial getTasks() call
     */
    LaunchedEffect(role, currentProject, projectsMap) {
        if (!ranOnce && role != null && currentProject != null && !projectsMap.isNullOrEmpty()) {

            Log.d("HomeScreen", "Project IDs map: $projectIds")
            selected = projectIds[currentProject].toString()

            ranOnce = true
            viewModel.getTasks(role = role, projectId = currentProject)
        }
    }

    /**
     * Handle auth errors for tasks loading:
     *  - If token is expired -> call refreshBlocking()
     *  - On successful refresh -> retry getTasks()
     *  - On refresh failure -> logout and navigate to login (clearing back stack)
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

    // Consume the generic "success" flag for tasks once it was observed
    LaunchedEffect(uiState.tasks.success) {
        viewModel.consumeSuccess()
    }

    /**
     * When tasks response changes:
     *  - store it locally in [tasks]
     *  - update [taskStatistic] using ViewModel's countTasks()
     */
    LaunchedEffect(uiState.tasks.response) {
        uiState.tasks.response?.let {
            tasks = it
            taskStatistic = viewModel.countTasks(tasks.user_tasks)
        }
    }

    // -------------------------------
    // UI LAYOUT
    // -------------------------------
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Screen title
        Text(
            "Home",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp
        )

        Spacer(Modifier.height(28.dp))

        // Project dropdown (only for Manager role)
        if (role == "Manager") {

            ItemDropdown(
                options = projectNames,
                selected = selected,
                onSelected = { newSelection ->
                    if (!projectsMap.isNullOrEmpty()) {
                        selected = newSelection

                        // Update current project in UserManager and reload tasks
                        projectsMap[newSelection]?.let { projectId ->
                            viewModel.updateCurrentProject(projectId)
                            viewModel.getTasks(role = role, projectId = projectId)
                        }
                    }
                },
                text = "Project"
            )

            Spacer(Modifier.height(12.dp))
        }

        // Navigate to Account screen
        AsanaButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Account",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 24,
            isIcon = true,
            icon = Icons.Default.AccountCircle,
            onClickAction = { navController.navigate("account") }
        )

        Spacer(Modifier.height(12.dp))

        // Navigate to Tasks screen
        AsanaButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Tasks",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 24,
            isIcon = true,
            icon = Icons.Default.CheckCircle,
            onClickAction = { navController.navigate("tasks") }
        )

        Spacer(Modifier.height(35.dp))

        // Tasks stats header
        Text(
            "Tasks",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            modifier = Modifier.padding(start = 0.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Row with three stats columns: Open / Completed / Total
        Row(modifier = Modifier.fillMaxWidth()) {

            // Open tasks
            Column(
                modifier = Modifier.weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                taskStatistic["open"]?.let {
                    Text(
                        it,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }
                Text(
                    "Open",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                )
            }

            // Completed tasks
            Column(
                modifier = Modifier.weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                taskStatistic["completed"]?.let {
                    Text(
                        it,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }
                Text(
                    "Completed",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                )
            }

            // Total tasks
            Column(
                modifier = Modifier.weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                taskStatistic["total"]?.let {
                    Text(
                        it,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }
                Text(
                    "Total Tasks",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Logout button
        AsanaButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Log out",
            paddingStart = 50,
            paddingEnd = 50,
            fontSize = 24,
            colorReversed = true,
            onClickAction = {
                scope.launch {
                    viewModel.logout()
                    navController.navigate("login") {
                        // Clear entire back stack and go to login
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            }
        )
    }
}