package com.euphoria.aimentor.data.repository

import com.euphoria.aimentor.data.api.ApiClient
import com.euphoria.aimentor.data.model.*

/**
 * AI Repository — all AI calls now go through the backend server.
 * No API key on the client. No JSON parsing on the client.
 * The backend handles OpenRouter communication, rate limiting, and validation.
 */
class AIRepository {

    private val api = ApiClient.apiService

    // ─── Analyze & Debug Code ─────────────────────────────────────────
    suspend fun analyzeCode(code: String, language: String): Result<String> {
        return try {
            val response = api.analyzeCode(CodeRequest(code, language))
            if (response.success && response.data != null) {
                Result.success(response.data.result)
            } else {
                Result.failure(Exception(response.error?.message ?: "Analysis failed"))
            }
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    // ─── Explain Code Step-by-Step ────────────────────────────────────
    suspend fun explainCode(code: String, language: String, beginnerMode: Boolean): Result<String> {
        return try {
            val response = api.explainCode(ExplainRequest(code, language, beginnerMode))
            if (response.success && response.data != null) {
                Result.success(response.data.result)
            } else {
                Result.failure(Exception(response.error?.message ?: "Explain failed"))
            }
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    // ─── Code Complexity Analysis ─────────────────────────────────────
    suspend fun analyzeComplexity(code: String, language: String): Result<String> {
        return try {
            val response = api.analyzeComplexity(CodeRequest(code, language))
            if (response.success && response.data != null) {
                Result.success(response.data.result)
            } else {
                Result.failure(Exception(response.error?.message ?: "Complexity analysis failed"))
            }
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    // ─── Generate Test Cases ──────────────────────────────────────────
    suspend fun generateTestCases(code: String, language: String): Result<String> {
        return try {
            val response = api.generateTests(CodeRequest(code, language))
            if (response.success && response.data != null) {
                Result.success(response.data.result)
            } else {
                Result.failure(Exception(response.error?.message ?: "Test generation failed"))
            }
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    // ─── Generate Quiz (pre-parsed by server) ─────────────────────────
    suspend fun generateQuiz(code: String, language: String): Result<List<QuizQuestion>> {
        return try {
            val response = api.generateQuiz(CodeRequest(code, language))
            if (response.success && response.data != null) {
                Result.success(response.data.questions)
            } else {
                Result.failure(Exception(response.error?.message ?: "Quiz generation failed"))
            }
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    // ─── Generate Learning Roadmap (pre-parsed by server) ─────────────
    suspend fun generateRoadmap(topic: String, level: String): Result<LearningRoadmap> {
        return try {
            val response = api.generateRoadmap(RoadmapRequest(topic, level))
            if (response.success && response.data?.roadmap != null) {
                Result.success(response.data.roadmap)
            } else {
                Result.failure(Exception(response.error?.message ?: "Roadmap generation failed"))
            }
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    // ─── Generate Interview Questions ─────────────────────────────────
    suspend fun generateInterviewQuestions(code: String, language: String): Result<String> {
        return try {
            val response = api.generateInterviewQuestions(CodeRequest(code, language))
            if (response.success && response.data != null) {
                Result.success(response.data.result)
            } else {
                Result.failure(Exception(response.error?.message ?: "Interview Q generation failed"))
            }
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    // ─── Chat (multi-turn) ────────────────────────────────────────────
    suspend fun chat(
        userMessage: String,
        history: List<ChatMessage>,
        currentCode: String?
    ): Result<String> {
        return try {
            val chatHistory = history.takeLast(10).map { msg ->
                ChatHistoryItem(
                    role = if (msg.role == MessageRole.USER) "USER" else "ASSISTANT",
                    content = msg.content
                )
            }
            val response = api.chat(
                ChatRequest(
                    message = userMessage,
                    history = chatHistory,
                    currentCode = currentCode
                )
            )
            if (response.success && response.data != null) {
                Result.success(response.data.reply)
            } else {
                Result.failure(Exception(response.error?.message ?: "Chat failed"))
            }
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    // ─── Error wrapping ───────────────────────────────────────────────
    private fun wrapError(e: Exception): Exception {
        val message = when {
            e.message?.contains("429") == true -> "Rate limit exceeded. Please wait a minute and try again."
            e.message?.contains("502") == true -> "AI is busy — free tier quota may be exhausted. Please wait a moment and retry."
            e.message?.contains("503") == true -> "AI service temporarily unavailable. Please try again shortly."
            e.message?.contains("401") == true -> "Session expired. Please sign in again."
            e.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out. Please try with shorter code."
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "No internet connection. Please check your network."
            else -> e.message ?: "Something went wrong. Please try again."
        }
        return Exception(message)
    }
}
