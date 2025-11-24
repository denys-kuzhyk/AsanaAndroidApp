package com.example.asanaapp.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asanaapp.data.model.AuthResponseSuccess
import com.example.asanaapp.data.model.ChangePasswordResponse
import com.example.asanaapp.data.model.StatusResponse
import com.example.asanaapp.data.model.TasksResponse
import com.example.asanaapp.data.model.UserTask
import com.example.asanaapp.data.network.AuthRepository
import com.example.asanaapp.data.storage.UserManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Main ViewModel for the app
 *
 * Responsibilities:
 *  - Holds UI state for:
 *      * refreshing tokens
 *      * fetching tasks
 *      * creating / editing / deleting tasks
 *      * changing password
 *  - Talks to [AuthRepository] for all backend calls
 *  - Exposes current project and role via [UserManager] as StateFlows
 *  - Centralizes retry logic when token is expired (refresh + retry pattern)
 */
class HomeViewModel(
    private val repository: AuthRepository,
    private val userManager: UserManager
) : ViewModel() {

    /**
     * Aggregated UI state for all operations on the Home screen area.
     */
    var uiState by mutableStateOf(HomeUiState())
        private set

    /**
     * Update the currently selected project (persisted via [UserManager]).
     */
    fun updateCurrentProject(projectId: String) {
        viewModelScope.launch {
            userManager.updateCurrentProject(projectId)
        }
    }

    /**
     * Fetch tasks for given [role] and [projectId]
     *
     * Pattern:
     *  1. Set loading state
     *  2. Call repository.getTasks()
     *  3. If success -> update success state
     *  4. If error and it's auth-related -> try repository.refresh() and retry once
     *  5. Otherwise -> propagate error message
     */
    fun getTasks(role: String, projectId: String) = viewModelScope.launch {
        uiState = uiState.copy(tasks = uiState.tasks.copy(isLoading = true, error = null))

        fun success(data: TasksResponse) {
            uiState = uiState.copy(
                tasks = uiState.tasks.copy(
                    isLoading = false,
                    success = true,
                    response = data,
                    error = null
                )
            )
        }

        fun fail(msg: String?) {
            uiState = uiState.copy(
                tasks = uiState.tasks.copy(
                    isLoading = false,
                    success = false,
                    error = msg
                )
            )
        }

        // 1) First attempt
        val first = repository.getTasks(role = role, projectId = projectId)
        if (first.isSuccess) {
            success(first.getOrNull()!!)
            return@launch
        }

        val err = first.exceptionOrNull()
        val authError = isAuthError(err)
        if (!authError) {
            fail(err?.message)
            return@launch
        }

        // 2) Token expired/401 -> refresh
        val ref = repository.refresh()
        if (ref.isFailure) {
            fail(ref.exceptionOrNull()?.message ?: "Refresh failed")
            return@launch
        }

        // 3) Retry after refresh
        val second = repository.getTasks(role = role, projectId = projectId)
        if (second.isSuccess) {
            success(second.getOrNull()!!)
        } else {
            fail(second.exceptionOrNull()?.message)
        }
    }

    /**
     * Edit an existing task
     *
     * Uses the same retry pattern as [getTasks] for auth errors
     */
    fun editTask(
        taskId: String,
        dueDate: String,
        status: String,
        projectId: String,
        assignee: String
    ) = viewModelScope.launch {
        uiState = uiState.copy(editTask = uiState.editTask.copy(isLoading = true, error = null))

        fun success(data: StatusResponse) {
            uiState = uiState.copy(
                editTask = uiState.editTask.copy(
                    isLoading = false,
                    success = true,
                    response = data,
                    error = null
                )
            )
        }

        fun fail(msg: String?) {
            uiState = uiState.copy(
                editTask = uiState.editTask.copy(
                    isLoading = false,
                    success = false,
                    error = msg
                )
            )
        }

        // 1) First attempt
        val first = repository.editTask(taskId, dueDate, status, projectId, assignee)
        if (first.isSuccess) {
            success(first.getOrNull()!!)
            return@launch
        }

        val err = first.exceptionOrNull()
        val authError = isAuthError(err)
        if (!authError) {
            fail(err?.message)
            return@launch
        }

        // 2) Token expired/401 -> refresh
        val ref = repository.refresh()
        if (ref.isFailure) {
            fail(ref.exceptionOrNull()?.message ?: "Refresh failed")
            return@launch
        }

        // 3) Retry after refresh
        val second = repository.editTask(taskId, dueDate, status, projectId, assignee)
        if (second.isSuccess) {
            success(second.getOrNull()!!)
        } else {
            fail(second.exceptionOrNull()?.message)
        }
    }

    /**
     * Create a new task
     *
     * Uses the same token refresh + retry logic as [editTask]
     */
    fun createTask(
        dueDate: String,
        status: String,
        projectId: String,
        name: String,
        description: String,
        assignee: String
    ) = viewModelScope.launch {
        uiState = uiState.copy(createTask = uiState.createTask.copy(isLoading = true, error = null))

        fun success(data: StatusResponse) {
            uiState = uiState.copy(
                createTask = uiState.createTask.copy(
                    isLoading = false,
                    success = true,
                    response = data,
                    error = null
                )
            )
        }

        fun fail(msg: String?) {
            uiState = uiState.copy(
                createTask = uiState.createTask.copy(
                    isLoading = false,
                    success = false,
                    error = msg
                )
            )
        }

        // 1) First attempt
        val first = repository.createTask(
            dueDate = dueDate,
            status = status,
            projectId = projectId,
            name = name,
            description = description,
            assignee = assignee
        )
        if (first.isSuccess) {
            success(first.getOrNull()!!)
            return@launch
        }

        val err = first.exceptionOrNull()
        val authError = isAuthError(err)
        if (!authError) {
            fail(err?.message)
            return@launch
        }

        // 2) Token expired/401 -> refresh
        val ref = repository.refresh()
        if (ref.isFailure) {
            fail(ref.exceptionOrNull()?.message ?: "Refresh failed")
            return@launch
        }

        // 3) Retry after refresh
        val second = repository.createTask(
            dueDate = dueDate,
            status = status,
            projectId = projectId,
            name = name,
            description = description,
            assignee = assignee
        )
        if (second.isSuccess) {
            success(second.getOrNull()!!)
        } else {
            fail(second.exceptionOrNull()?.message)
        }
    }

    /**
     * Delete a task by ID (internal helper)
     *
     * Uses the same retry logic on auth errors as other operations
     */
    private fun deleteTask(taskId: String) = viewModelScope.launch {
        uiState = uiState.copy(deleteTask = uiState.deleteTask.copy(isLoading = true, error = null))

        fun success(data: StatusResponse) {
            uiState = uiState.copy(
                deleteTask = uiState.deleteTask.copy(
                    isLoading = false,
                    success = true,
                    response = data,
                    error = null
                )
            )
        }

        fun fail(msg: String?) {
            uiState = uiState.copy(
                deleteTask = uiState.deleteTask.copy(
                    isLoading = false,
                    success = false,
                    error = msg
                )
            )
        }

        // 1) First attempt
        val first = repository.deleteTask(taskId = taskId)
        if (first.isSuccess) {
            success(first.getOrNull()!!)
            return@launch
        }

        val err = first.exceptionOrNull()
        val authError = isAuthError(err)
        if (!authError) {
            fail(err?.message)
            return@launch
        }

        // 2) Token expired/401 -> refresh
        val ref = repository.refresh()
        if (ref.isFailure) {
            fail(ref.exceptionOrNull()?.message ?: "Refresh failed")
            return@launch
        }

        // 3) Retry after refresh
        val second = repository.deleteTask(taskId = taskId)
        if (second.isSuccess) {
            success(second.getOrNull()!!)
        } else {
            fail(second.exceptionOrNull()?.message)
        }
    }

    /**
     * Change user's password
     *
     * Follows the same refresh + retry flow as other operations
     */
    fun changePassword(password: String, newPassword: String) = viewModelScope.launch {
        uiState = uiState.copy(changePassword = uiState.changePassword.copy(isLoading = true, error = null))

        fun success(data: ChangePasswordResponse) {
            uiState = uiState.copy(
                changePassword = uiState.changePassword.copy(
                    isLoading = false,
                    success = true,
                    response = data,
                    error = null
                )
            )
        }

        fun fail(msg: String?) {
            uiState = uiState.copy(
                changePassword = uiState.changePassword.copy(
                    isLoading = false,
                    success = false,
                    error = msg
                )
            )
        }

        // 1) First attempt
        val first = repository.changePassword(password, newPassword)
        if (first.isSuccess) {
            success(first.getOrNull()!!)
            return@launch
        }

        val err = first.exceptionOrNull()
        val authError = isAuthError(err)
        if (!authError) {
            fail(err?.message)
            return@launch
        }

        // 2) Token expired/401 -> refresh
        val ref = repository.refresh()
        if (ref.isFailure) {
            fail(ref.exceptionOrNull()?.message ?: "Refresh failed")
            return@launch
        }

        // 3) Retry after refresh
        val second = repository.changePassword(password, newPassword)
        if (second.isSuccess) {
            success(second.getOrNull()!!)
        } else {
            fail(second.exceptionOrNull()?.message)
        }
    }

    /**
     * Helper to detect whether an error is authentication-related
     *
     * - HTTP 401
     * - or specific token error messages from backend
     */
    private fun isAuthError(t: Throwable?): Boolean {
        return when (t) {
            is HttpException -> t.code() == 401
            else -> {
                val m = t?.message
                m == "Token has expired" || m == "Token is not valid anymore"
            }
        }
    }

    /**
     * Count tasks by status
     *
     * @return map with:
     *  - "open"      -> number of non-"Completed" tasks
     *  - "completed" -> number of "Completed" tasks
     *  - "total"     -> total count
     */
    fun countTasks(tasks: List<UserTask>): Map<String, String> {

        var open = 0
        var completed = 0
        var total = 0

        for (task in tasks) {
            if (task.status != "Completed") {
                open++
            } else {
                completed++
            }
            total++
        }

        return mapOf(
            "open" to open.toString(),
            "completed" to completed.toString(),
            "total" to total.toString()
        )
    }

    /**
     * Perform a blocking (from caller perspective) refresh call
     * and update [uiState.refresh] with loading/success/error
     *
     * Returns [Result<Unit>] indicating success/failure of refresh
     */
    suspend fun refreshBlocking(): Result<Unit> {

        uiState = uiState.copy(
            refresh = uiState.refresh.copy(isLoading = true, error = null, success = false)
        )

        val result = repository.refresh()
        uiState = if (result.isSuccess) {
            uiState.copy(
                refresh = uiState.refresh.copy(
                    isLoading = false,
                    success = true,
                    refreshResponse = result.getOrNull(),
                    error = null
                )
            )
        } else {
            uiState.copy(
                refresh = uiState.refresh.copy(
                    isLoading = false,
                    success = false,
                    error = result.exceptionOrNull()?.message
                )
            )
        }

        return result.map { }
    }

    /**
     * Public handler for delete button click on a task.
     *
     * Flow:
     *  1. Call [deleteTask] for given [taskId]
     *  2. Wait 2 seconds
     *  3. Refresh task list with [getTasks] using provided [role] and [projectId]
     *
     * (The delay is a small buffer to ensure backend has processed deletion.)
     */
    fun onDeleteTaskClicked(taskId: String, role: String, projectId: String) {
        viewModelScope.launch {
            deleteTask(taskId = taskId)

            delay(timeMillis = 2000)

            getTasks(
                role = role,
                projectId = projectId
            )
        }
    }

    /**
     * Logout and clear all user/auth data at repository level
     */
    suspend fun logout() {
        repository.logout()
    }

    /**
     * Clear all error messages in [uiState] (for all operations)
     */
    fun clearError() {
        uiState = uiState.copy(
            refresh = uiState.refresh.copy(error = null),
            tasks = uiState.tasks.copy(error = null),
            editTask = uiState.editTask.copy(error = null),
            changePassword = uiState.changePassword.copy(error = null),
            createTask = uiState.createTask.copy(error = null),
            deleteTask = uiState.deleteTask.copy(error = null)
        )
    }

    /**
     * Clear all responses in [uiState] (for all operations)
     */
    fun clearResponse() {
        uiState = uiState.copy(
            refresh = uiState.refresh.copy(refreshResponse = null),
            tasks = uiState.tasks.copy(response = null),
            editTask = uiState.editTask.copy(response = null),
            changePassword = uiState.changePassword.copy(response = null),
            createTask = uiState.createTask.copy(response = null),
            deleteTask = uiState.deleteTask.copy(response = null)
        )
    }

    /**
     * Reset all `success` flags to false after they were consumed by the UI
     *
     * Prevents repeated reactions to the same success event (e.g. dialog reopening)
     */
    fun consumeSuccess() {
        uiState = uiState.copy(
            refresh = uiState.refresh.copy(success = false),
            tasks = uiState.tasks.copy(success = false),
            editTask = uiState.editTask.copy(success = false),
            changePassword = uiState.changePassword.copy(success = false),
            createTask = uiState.createTask.copy(success = false),
            deleteTask = uiState.deleteTask.copy(success = false)
        )
    }
}

/**
 * UI sub-state for token refresh operation
 */
data class RefreshUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val refreshResponse: AuthResponseSuccess? = null
)

/**
 * UI sub-state for "get tasks" operation
 */
data class TasksUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val response: TasksResponse? = null
)

/**
 * UI sub-state for "edit task" operation
 */
data class EditTaskUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val response: StatusResponse? = null
)

/**
 * UI sub-state for "change password" operation
 */
data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val response: ChangePasswordResponse? = null
)

/**
 * UI sub-state for "create task" operation
 */
data class CreateTaskUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val response: StatusResponse? = null
)

/**
 * UI sub-state for "delete task" operation
 */
data class DeleteTaskUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val response: StatusResponse? = null
)

/**
 * Top-level UI state that aggregates all operations handled in [HomeViewModel]
 */
data class HomeUiState(
    val refresh: RefreshUiState = RefreshUiState(),
    val tasks: TasksUiState = TasksUiState(),
    val editTask: EditTaskUiState = EditTaskUiState(),
    val changePassword: ChangePasswordUiState = ChangePasswordUiState(),
    val createTask: CreateTaskUiState = CreateTaskUiState(),
    val deleteTask: DeleteTaskUiState = DeleteTaskUiState()
)