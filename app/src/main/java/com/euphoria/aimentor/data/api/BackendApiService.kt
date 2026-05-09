package com.euphoria.aimentor.data.api

import com.euphoria.aimentor.data.model.*
import retrofit2.http.*

/**
 * Backend REST API service.
 * All AI calls are proxied through the backend server which holds the OpenRouter API key.
 */
interface BackendApiService {

    // ─── AI Endpoints ─────────────────────────────────────────────────
    @POST("api/ai/analyze")
    suspend fun analyzeCode(@Body request: CodeRequest): ApiResponse<AiTextResult>

    @POST("api/ai/explain")
    suspend fun explainCode(@Body request: ExplainRequest): ApiResponse<AiTextResult>

    @POST("api/ai/complexity")
    suspend fun analyzeComplexity(@Body request: CodeRequest): ApiResponse<AiTextResult>

    @POST("api/ai/tests")
    suspend fun generateTests(@Body request: CodeRequest): ApiResponse<AiTextResult>

    @POST("api/ai/quiz")
    suspend fun generateQuiz(@Body request: CodeRequest): ApiResponse<QuizResult>

    @POST("api/ai/roadmap")
    suspend fun generateRoadmap(@Body request: RoadmapRequest): ApiResponse<RoadmapResult>

    @POST("api/ai/interview")
    suspend fun generateInterviewQuestions(@Body request: CodeRequest): ApiResponse<AiTextResult>

    @POST("api/ai/chat")
    suspend fun chat(@Body request: ChatRequest): ApiResponse<ChatResult>

    // ─── Data Endpoints ───────────────────────────────────────────────
    @GET("api/data/history")
    suspend fun getHistory(): ApiResponse<List<CodeHistoryItem>>

    @POST("api/data/history")
    suspend fun saveHistoryItem(@Body item: CodeHistoryItem): ApiResponse<CodeHistoryItem>

    @DELETE("api/data/history/{id}")
    suspend fun deleteHistoryItem(@Path("id") id: String): ApiResponse<Any>

    @GET("api/data/leaderboard")
    suspend fun getLeaderboard(): ApiResponse<List<LeaderboardEntry>>

    @GET("api/data/challenge")
    suspend fun getDailyChallenge(): ApiResponse<DailyChallenge>

    // ─── User Endpoints ───────────────────────────────────────────────
    @GET("api/user/profile")
    suspend fun getProfile(): ApiResponse<UserProfile>

    @PUT("api/user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<Any>

    @GET("api/user/progress")
    suspend fun getProgress(): ApiResponse<LearningProgress>

    // ─── Health Check ─────────────────────────────────────────────────
    @GET("api/health")
    suspend fun healthCheck(): ApiResponse<HealthStatus>
}
