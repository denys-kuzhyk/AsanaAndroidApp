package com.example.asanaapp.data.network

import com.example.asanaapp.data.model.AuthRequest
import com.example.asanaapp.data.model.AuthResponseError
import com.example.asanaapp.data.model.AuthResponseSuccess
import com.example.asanaapp.data.model.ChangePasswordRequest
import com.example.asanaapp.data.model.ChangePasswordResponse
import com.example.asanaapp.data.model.CreateTaskRequest
import com.example.asanaapp.data.model.DeleteTaskRequest
import com.example.asanaapp.data.model.EditTasksRequest
import com.example.asanaapp.data.model.ResponseError
import com.example.asanaapp.data.model.StatusResponse
import com.example.asanaapp.data.model.TasksResponse
import com.example.asanaapp.data.storage.AuthManager
import com.example.asanaapp.data.storage.TokenManager
import com.example.asanaapp.data.storage.UserManager
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository responsible for:
 * - calling Auth / Task endpoints via [ApiService]
 * - handling token storage with [tokenManager] / refresh
 * - saving basic user info via [userManager]
 * - mapping raw API errors into Kotlin [Result] objects
 *
 * This is the main entry point for ViewModels that need to talk to the backend
 */
class AuthRepository(
    private val tokenManager: TokenManager,
    private val authManager: AuthManager,
    private val userManager: UserManager
) {

    // Retrofit API interface instance
    private val api = RetrofitInstance.api

    /**
     * Login with email + password
     *
     * On success:
     *  - marks user as logged in
     *  - saves access/refresh tokens
     *  - saves user profile (id, name, email, role, projects)
     *
     * On failure:
     *  - tries to parse error body into [AuthResponseError]
     *  - wraps message in a failed [Result]
     */
    suspend fun login(request: AuthRequest): Result<AuthResponseSuccess> {
        return try {
            val response = api.login(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Mark user as logged in
                authManager.setLoggedIn()

                // Store tokens securely
                tokenManager.saveTokens(body.access_token, body.refresh_token)

                // Saves user info locally
                userManager.saveUser(
                    id = body.id,
                    name = body.name,
                    email = body.email,
                    role = body.role,
                    currentProject = body.project_id.split(",")[0], // take first project as the current one
                    projectsMap = body.project_names
                )

                Result.success(body)
            } else {
                // Parse backend error message (if possible)
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, AuthResponseError::class.java).msg
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            // Network issues: no internet, timeout, etc.
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            // Non-2xx HTTP status codes
            Result.failure(Exception("Server error: ${e.message()}"))
        }
    }

    /**
     * Signup
     *
     * Flow is similar to [login]:
     *  - if successful: store tokens + user info and mark logged in
     *  - if failed: parse [AuthResponseError] from error body when possible
     */
    suspend fun signUp(request: AuthRequest): Result<AuthResponseSuccess> {
        return try {
            val response = api.signUp(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                authManager.setLoggedIn()
                tokenManager.saveTokens(body.access_token, body.refresh_token)

                userManager.saveUser(
                    id = body.id,
                    name = body.name,
                    email = body.email,
                    role = body.role,
                    currentProject = body.project_id.split(",")[0],
                    projectsMap = body.project_names
                )

                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, AuthResponseError::class.java).msg
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message()}"))
        }
    }

    /**
     * Refresh access/refresh tokens using the stored refresh token
     *
     * On success: overwrites both tokens via [TokenManager]
     * On failure: parses [AuthResponseError] if possible
     */
    suspend fun refresh(): Result<AuthResponseSuccess> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            val response = api.refresh("Bearer $refreshToken")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Save new tokens, preserving authenticated session
                tokenManager.saveTokens(body.access_token, body.refresh_token)

                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, AuthResponseError::class.java).msg
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message()}"))
        }
    }

    /**
     * Fetch tasks for given [role] and [projectId]
     *
     * Uses the stored access token for authorization
     * On success:
     *  - updates statuses in [UserManager]
     *  - returns tasks list
     */
    suspend fun getTasks(role: String, projectId: String): Result<TasksResponse> {
        return try {
            val accessToken = tokenManager.getAccessToken()

            val response = api.getTasks(
                bearer = "Bearer $accessToken",
                role = role,
                project = projectId
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Save available statuses locally
                userManager.updateStatuses(body.statuses)

                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, ResponseError::class.java).msg
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message()}"))
        }
    }

    /**
     * Edit a task with given parameters
     *
     * Wraps parameters into [EditTasksRequest] and sends it to the API
     */
    suspend fun editTask(
        taskId: String,
        dueDate: String,
        status: String,
        projectId: String,
        assignee: String
    ): Result<StatusResponse> {
        return try {
            val accessToken = tokenManager.getAccessToken()
            val response = api.editTask(
                "Bearer $accessToken",
                EditTasksRequest(taskId, dueDate, status, projectId, assignee)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, ResponseError::class.java).msg
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message()}"))
        }
    }

    /**
     * Create a new task
     *
     * Builds a [CreateTaskRequest] from parameters and calls the API
     */
    suspend fun createTask(
        dueDate: String,
        status: String,
        projectId: String,
        name: String,
        description: String,
        assignee: String
    ): Result<StatusResponse> {
        return try {
            val accessToken = tokenManager.getAccessToken()
            val response = api.createTask(
                bearer = "Bearer $accessToken",
                request = CreateTaskRequest(
                    due_date = dueDate,
                    status = status,
                    project_id = projectId,
                    name = name,
                    description = description,
                    assignee = assignee
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, ResponseError::class.java).msg
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message()}"))
        }
    }

    /**
     * Delete a task by [taskId]
     */
    suspend fun deleteTask(taskId: String): Result<StatusResponse> {
        return try {
            val accessToken = tokenManager.getAccessToken()
            val response = api.deleteTask(
                bearer = "Bearer $accessToken",
                request = DeleteTaskRequest(task_id = taskId)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, ResponseError::class.java).msg
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message()}"))
        }
    }

    /**
     * Change current user's password
     */
    suspend fun changePassword(
        password: String,
        newPassword: String
    ): Result<ChangePasswordResponse> {
        return try {
            val accessToken = tokenManager.getAccessToken()
            val response = api.changePassword(
                "Bearer $accessToken",
                ChangePasswordRequest(password, newPassword)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, ResponseError::class.java).msg
                } catch (e: Exception) {
                    "Unknown error"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: HttpException) {
            Result.failure(Exception("Server error: ${e.message()}"))
        }
    }

    /**
     * Clear all authentication-related data:
     * - tokens
     * - auth flag
     * - user info
     *
     * Called on logout
     */
    suspend fun logout() {
        tokenManager.clear()
        authManager.setLoggedOut()
        userManager.clear()
    }
}
