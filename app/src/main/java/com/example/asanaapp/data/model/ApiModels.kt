package com.example.asanaapp.data.model

import com.google.gson.annotations.SerializedName

// ==========================
// Request Models (APP ➜ API)
// ==========================

/**
 * Body for login / sign up request
 */
data class AuthRequest(
    val email: String,   // User's email
    val password: String // Plain password entered by the user
)

/**
 * Body for editing an existing task
 */
data class EditTasksRequest(
    val task_id: String,   // ID of the task to update (Asana task gid)
    val due_date: String,  // New due date (string format "YYYY-MM-DD")
    val status: String,    // New status
    val project_id: String,// Project this task belongs to (Asana project gid)
    val assignee: String   // Email of the assignee
)

/**
 * Body for creating a new task
 */
data class CreateTaskRequest(
    val due_date: String,    // Due date for the new task
    val status: String,      // Initial status for the task
    val project_id: String,  // Project the task is created in (Asana project gid)
    val name: String,        // Task name
    val description: String, // Description of the task
    val assignee: String     // Email of the assignee
)

/**
 * Body for deleting a task
 */
data class DeleteTaskRequest(
    val task_id: String // ID of the task to delete (Asana task gid)
)

/**
 * Body for changing the user's password
 */
data class ChangePasswordRequest(
    val password: String,      // Current password
    val new_password: String   // New password to be set
)


// ==========================
// Response Models (API ➜ APP)
// ==========================

/**
 * Successful login / sign up response
 * Contains JWT tokens and basic user info
 */
data class AuthResponseSuccess(
    val access_token: String,          // JWT access token
    val access_token_expires: Int,     // Access token expiry date (unix format)
    val msg: String,                   // Message from backend
    val refresh_token: String,         // JWT refresh token
    val refresh_token_expires: Int,    // Refresh token expiry date (unix format)
    val email: String,                 // User's email
    val name: String,                  // User's name
    val id: String,                    // User id (Asana task gid from Users List project)
    val role: String,                  // User role
    val project_id: String,            // Currently selected / default project id (Asana project gid)
    val project_names: Map<String, String> = mapOf() // Map<projectName to projectId>
)

/**
 * Failed login / sign up response
 */
data class AuthResponseError(
    val msg: String // Error message
)

/**
 * Generic error response used by various endpoints
 */
data class ResponseError(
    val msg: String // Error message
)

/**
 * Single task item returned from the backend
 * Field names are mapped to JSON keys using @SerializedName
 */
data class UserTask(
    @SerializedName("AssigneeID")
    val assigneeId: String = "", // ID / email of the assignee
    @SerializedName("Status")
    val status: String = "", // Task status (custom Asana column)
    @SerializedName("due_date")
    val dueDate: String = "", // Due date (unix format)
    @SerializedName("name")
    val name: String = "", // Task name
    @SerializedName("task_id")
    val taskId: String = "", // ID of the task (Asana task gid)
    @SerializedName("TaskDescription")
    val description: String = "" // Description of the task
)

/**
 * Response for "get-tasks" endpoint
 */
data class TasksResponse(
    val msg: String = "",           // Message from Flask
    val user_tasks: List<UserTask>, // List of tasks for current user / project
    val statuses: String            // String list of statuses in the current project separated by a comma
)

/**
 * Simple response for status-related operations used in task manupulation operations
 */
data class StatusResponse(
    val msg: String = "" // Message from Flask
)

/**
 * Response after changing the user's password
 */
data class ChangePasswordResponse(
    val msg: String = "" // Success / error message
)