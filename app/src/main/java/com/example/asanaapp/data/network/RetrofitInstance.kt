package com.example.asanaapp.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton that provides a configured Retrofit instance
 * and exposes the [ApiService] implementation
 *
 * This is the central place where we:
 *  - define BASE_URL (Flask backend URL)
 *  - configure JSON converter (Gson)
 *  - create the Retrofit service interface
 */
object RetrofitInstance {

    // Base URL for the Flask backend
    private const val BASE_URL = "http://10.0.2.2:5000/"

    /**
     * Lazily initialized Retrofit instance
     *
     * - baseUrl: root URL for all API calls
     * - addConverterFactory: tells Retrofit to use Gson for JSON <-> Kotlin object conversion
     */
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazily created implementation of [ApiService]
     *
     * Usage:
     *   RetrofitInstance.api.login(...)
     *   RetrofitInstance.api.getTasks(...)
     */
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}