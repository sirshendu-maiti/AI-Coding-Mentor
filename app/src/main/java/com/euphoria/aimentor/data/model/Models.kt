package com.euphoria.aimentor.data.model

import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════
// Backend API Wrapper
// ═══════════════════════════════════════════════════════════════════════

/** Standard backend response wrapper */
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String = "",
    val message: String = "",
    val retryAfter: Int? = null
)

data class HealthStatus(
    val status: String = "",
    val version: String = "",
    val timestamp: String = "",
    val uptime: String = ""
)

// ═══════════════════════════════════════════════════════════════════════
// Backend Request Models
// ═══════════════════════════════════════════════════════════════════════

data class CodeRequest(
    val code: String,
    val language: String
)

data class ExplainRequest(
    val code: String,
    val language: String,
    val beginnerMode: Boolean = false
)

data class ChatRequest(
    val message: String,
    val history: List<ChatHistoryItem> = emptyList(),
    val currentCode: String? = null
)

data class ChatHistoryItem(
    val role: String = "",  // "USER" or "ASSISTANT"
    val content: String = ""
)

data class RoadmapRequest(
    val topic: String,
    val level: String  // "Beginner", "Intermediate", "Advanced"
)

data class UpdateProfileRequest(
    val displayName: String
)

// ═══════════════════════════════════════════════════════════════════════
// Backend Response Models (pre-parsed by server)
// ═══════════════════════════════════════════════════════════════════════

data class AiTextResult(
    val result: String = ""
)

data class QuizResult(
    val questions: List<QuizQuestion> = emptyList()
)

data class RoadmapResult(
    val roadmap: LearningRoadmap? = null
)

data class ChatResult(
    val reply: String = ""
)

// ═══════════════════════════════════════════════════════════════════════
// Chat / AI Messages
// ═══════════════════════════════════════════════════════════════════════

// FIX: Use String constants instead of Kotlin enum for Firebase serialization
// Firebase RTDB cannot deserialize Kotlin enums reliably — they cause
// DataSnapshot.getValue() to return null when the enum name doesn't match.
object MessageRole {
    const val USER = "USER"
    const val ASSISTANT = "ASSISTANT"
}

object MessageType {
    const val TEXT = "TEXT"
    const val CODE = "CODE"
    const val ERROR = "ERROR"
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String = MessageRole.USER,        // "USER" or "ASSISTANT"
    val content: String = "",
    val type: String = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String? = null
)

// ═══════════════════════════════════════════════════════════════════════
// Code Analysis
// ═══════════════════════════════════════════════════════════════════════

data class CodeError(
    val line: Int? = null,
    val type: String = "",
    val message: String = "",
    val suggestion: String = ""
)

data class CodeAnalysisResult(
    val originalCode: String = "",
    val language: String = "",
    val errors: List<CodeError> = emptyList(),
    val explanation: String = "",
    val optimizedCode: String? = null,
    val complexity: String? = null,
    val suggestions: List<String> = emptyList()
)

// ═══════════════════════════════════════════════════════════════════════
// Quiz / Learning
// ═══════════════════════════════════════════════════════════════════════

data class QuizOption(val label: String = "", val text: String = "", val isCorrect: Boolean = false)

data class QuizQuestion(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val options: List<QuizOption> = emptyList(),
    val explanation: String = ""
)

data class LearningProgress(
    val totalQuizzes: Int = 0,
    val correctAnswers: Int = 0,
    val streak: Int = 0,
    val lastActiveDate: Long = System.currentTimeMillis(),
    val languagesUsed: List<String> = emptyList()
)

// ═══════════════════════════════════════════════════════════════════════
// Roadmap
// ═══════════════════════════════════════════════════════════════════════

data class RoadmapStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val resources: List<String> = emptyList()
)

data class LearningRoadmap(
    val id: String = UUID.randomUUID().toString(),
    val topic: String = "",
    val level: String = "",
    val steps: List<RoadmapStep> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════════════
// Daily Challenge
// ═══════════════════════════════════════════════════════════════════════

data class DailyChallenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val difficulty: String = "Easy",
    val baseCode: String = "",
    val expectedOutput: String = ""
)

// ═══════════════════════════════════════════════════════════════════════
// Leaderboard
// ═══════════════════════════════════════════════════════════════════════

data class LeaderboardEntry(
    val uid: String = "",
    val displayName: String = "Anonymous",
    val score: Int = 0,
    val rank: Int = 0
)

// ═══════════════════════════════════════════════════════════════════════
// Code History
// ═══════════════════════════════════════════════════════════════════════

data class CodeHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val code: String = "",
    val language: String = "",
    val summary: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val hasErrors: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════
// User Profile
// ═══════════════════════════════════════════════════════════════════════

data class UserProfile(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════════════
// Input Modes & Languages
// ═══════════════════════════════════════════════════════════════════════

enum class InputMode { TEXT, CAMERA, VOICE }

enum class ProgrammingLanguage(val displayName: String, val extension: String) {
    AUTO("Auto Detect", ""),
    PYTHON("Python", "py"),
    JAVA("Java", "java"),
    KOTLIN("Kotlin", "kt"),
    JAVASCRIPT("JavaScript", "js"),
    C("C", "c"),
    CPP("C++", "cpp");

    companion object {
        fun fromCode(code: String): ProgrammingLanguage {
            return when {
                code.contains("def ") || (code.contains("import ") && code.contains(":")) -> PYTHON
                code.contains("public class") || code.contains("System.out") -> JAVA
                code.contains("fun ") && code.contains("val ") -> KOTLIN
                code.contains("function") || code.contains("const ") || code.contains("let ") -> JAVASCRIPT
                code.contains("#include") && code.contains("cout") -> CPP
                code.contains("#include") -> C
                else -> AUTO
            }
        }
    }
}
