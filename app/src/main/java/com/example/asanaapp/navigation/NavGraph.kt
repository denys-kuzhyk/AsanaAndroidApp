package com.example.asanaapp.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.asanaapp.data.storage.AuthManager
import com.example.asanaapp.data.storage.UserManager
import com.example.asanaapp.ui.app.HomeViewModel
import com.example.asanaapp.ui.app.Account
import com.example.asanaapp.ui.app.ChangePassword
import com.example.asanaapp.ui.app.EditTaskScreen
import com.example.asanaapp.ui.app.Home
import com.example.asanaapp.ui.app.NewTaskScreen
import com.example.asanaapp.ui.app.TasksScreen
import com.example.asanaapp.ui.auth.login.LoginScreen
import com.example.asanaapp.ui.auth.login.LoginViewModel
import com.example.asanaapp.ui.auth.signup.EmailScreen
import com.example.asanaapp.ui.auth.signup.PasswordScreen
import com.example.asanaapp.ui.auth.signup.SignUpViewModel

/**
 * Main navigation graph for the app
 *
 * Decides the start destination based on authentication state:
 *  - if user is logged in -> "homeScreen"
 *  - otherwise           -> "login"
 *
 * It wires all screens (login, signup, home, tasks, account, etc.)
 * and passes required ViewModels / managers as parameters
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel,
    signUpViewModel: SignUpViewModel,
    homeViewModel: HomeViewModel,
    authManager: AuthManager,
    userManager: UserManager
) {
    // Remember NavController for managing app navigation stack
    val navController = rememberNavController()

    // Observe login state from AuthManager
    val loggedIn by authManager.isLoggedIn.collectAsState()

    // Choose start destination based on login state
    val start = if (loggedIn) "homeScreen" else "login"

    // Root navigation host: defines all available routes
    NavHost(navController = navController, startDestination = start) {

        // -------------------------
        // AUTH FLOW
        // -------------------------

        // Login screen (first screen for non-authenticated users)
        composable("login") {
            LoginScreen(modifier, navController, viewModel = loginViewModel)
        }

        // Signup: step 1 — enter email
        composable("emailScreen") {
            EmailScreen(modifier, navController)
        }

        // Signup: step 2 — enter password (email passed as argument)
        composable(
            route = "passwordScreen/{email}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email").orEmpty()
            PasswordScreen(
                modifier = modifier,
                navController = navController,
                email = email,
                viewModel = signUpViewModel
            )
        }

        // -------------------------
        // MAIN APP (AFTER LOGIN)
        // -------------------------

        // Main home screen
        composable("homeScreen") {
            Home(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                userManager = userManager,
                viewModel = homeViewModel
            )
        }

        // Account details screen
        composable("account") {
            Account(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                userManager = userManager
            )
        }

        // Tasks list screen (for current project / role)
        composable(route = "tasks") {
            TasksScreen(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                viewModel = homeViewModel,
                userManager = userManager
            )
        }

        // Create new task screen
        composable(route = "newTask") {
            NewTaskScreen(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                viewModel = homeViewModel,
                userManager = userManager
            )
        }

        // -------------------------
        // EDIT TASK FLOW
        // -------------------------

        /**
         * Edit existing task screen
         *
         * Route arguments:
         *  - status   : current status of the task
         *  - dueDate  : current due date
         *  - taskId   : unique identifier of the task (Asana task gid)
         *  - assignee : current assignee
         *  - role     : current user role (affects edit options)
         */
        composable(
            route = "editTasks/{status}/{dueDate}/{taskId}/{assignee}/{role}",
            arguments = listOf(
                navArgument("status") { type = NavType.StringType },
                navArgument("dueDate") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.StringType },
                navArgument("assignee") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val status = backStackEntry.arguments?.getString("status").orEmpty()
            val dueDate = backStackEntry.arguments?.getString("dueDate").orEmpty()
            val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
            val assignee = backStackEntry.arguments?.getString("assignee").orEmpty()
            val role = backStackEntry.arguments?.getString("role").orEmpty()

            EditTaskScreen(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                existingDueDate = dueDate,
                selected = status,
                taskId = taskId,
                viewModel = homeViewModel,
                assignee = assignee,
                role = role,
                userManager = userManager
            )
        }

        // Change password screen (within authenticated area)
        composable("changePassword") {
            ChangePassword(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                viewModel = homeViewModel
            )
        }
    }
}