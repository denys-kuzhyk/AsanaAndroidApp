package com.example.asanaapp.data.network

import com.example.asanaapp.data.model.AuthRequest
import com.example.asanaapp.data.model.AuthResponseSuccess
import com.example.asanaapp.data.model.ChangePasswordRequest
import com.example.asanaapp.data.model.ChangePasswordResponse
import com.example.asanaapp.data.model.CreateTaskRequest
import com.example.asanaapp.data.model.DeleteTaskRequest
import com.example.asanaapp.data.model.EditTasksRequest
import com.example.asanaapp.data.model.StatusResponse
import com.example.asanaapp.data.model.TasksResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query


/**
 * Retrofit API definition for communication with the Flask server
 *
 * All functions are `suspend` so they can be called from coroutines (e.g. in a ViewModel)
 */
interface ApiService {

    /**
     * Login endpoint
     *
     * Sends user's credentials and expects a success response
     * with access/refresh tokens and user info
     */
    @POST("login")
    suspend fun login(
        @Body request: AuthRequest       // email + password
    ): Response<AuthResponseSuccess>

    /**
     * Signup endpoint
     *
     * Uses the same request body as login (email + password),
     * backend checks whether such email exists in Asana and creates a new account + returns tokens + user info
     */
    @POST("signup")
    suspend fun signUp(
        @Body request: AuthRequest
    ): Response<AuthResponseSuccess>

    /**
     * Refreshing JWT tokens using the current (valid) refresh token
     *
     * `bearer` should be in format: "Bearer <refresh_token>"
     */
    @POST("refresh")
    suspend fun refresh(
        @Header("Authorization") bearer: String
    ): Response<AuthResponseSuccess>

    /**
     * Get tasks for the current user / role / project
     *
     * `Authorization` header: "Bearer <access_token>"
     * `role` and `project_id` are passed as query parameters
     */
    @GET("get-tasks")
    suspend fun getTasks(
        @Header("Authorization") bearer: String,
        @Query("role") role: String,
        @Query("project_id") project: String  // Asana project gid
    ): Response<TasksResponse>

    /**
     * Edit an existing task
     *
     * Sends an `EditTasksRequest` with all fields required by the backend
     */
    @PUT("edit-task")
    suspend fun editTask(
        @Header("Authorization") bearer: String,
        @Body request: EditTasksRequest
    ): Response<StatusResponse>

    /**
     * Create a new task in a given project
     */
    @POST("create-task")
    suspend fun createTask(
        @Header("Authorization") bearer: String,
        @Body request: CreateTaskRequest
    ): Response<StatusResponse>

    /**
     * Delete a task
     *
     * Using @HTTP instead of @DELETE because we need to send a body (task_id)
     */
    @HTTP(method = "DELETE", path = "delete-task", hasBody = true)
    suspend fun deleteTask(
        @Header("Authorization") bearer: String,
        @Body request: DeleteTaskRequest
    ): Response<StatusResponse>

    /**
     * Change current user's password
     */
    @PUT("change-password")
    suspend fun changePassword(
        @Header("Authorization") bearer: String,
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>
}