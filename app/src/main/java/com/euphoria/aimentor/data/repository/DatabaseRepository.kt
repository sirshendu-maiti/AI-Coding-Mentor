package com.euphoria.aimentor.data.repository

import android.util.Log
import com.euphoria.aimentor.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DatabaseRepository {
    private val auth = FirebaseAuth.getInstance()

    // FIX: Safe lazy initialization — returns null if Firebase Realtime Database
    // is not configured. This prevents a crash if the URL is missing in google-services.json.
    private val database: DatabaseReference? by lazy {
        try {
            FirebaseDatabase.getInstance().reference
        } catch (e: Exception) {
            Log.w("DatabaseRepository", "Firebase Realtime Database not configured: ${e.message}")
            null
        }
    }

    private val userPath: String?
        get() = auth.currentUser?.uid

    // ─── History ──────────────────────────────────────────────────────

    suspend fun saveHistoryItem(item: CodeHistoryItem): Result<Unit> {
        val db = database ?: return Result.success(Unit) // Silently skip if DB not configured
        val path = userPath ?: return Result.failure(Exception("User not logged in"))
        return try {
            db.child("users").child(path).child("history").child(item.id).setValue(item).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseRepository", "saveHistoryItem failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun getHistoryFlow(): Flow<List<CodeHistoryItem>> = callbackFlow {
        val db = database
        val path = userPath
        if (db == null || path == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.child("users").child(path).child("history")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(CodeHistoryItem::class.java) }
                trySend(items.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseRepository", "getHistoryFlow cancelled: ${error.message}")
                trySend(emptyList()) // FIX: don't close with error, just send empty
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ─── Progress ─────────────────────────────────────────────────────

    suspend fun updateProgress(progress: LearningProgress): Result<Unit> {
        val db = database ?: return Result.success(Unit)
        val path = userPath ?: return Result.failure(Exception("User not logged in"))
        return try {
            db.child("users").child(path).child("progress").setValue(progress).await()

            // Also update leaderboard entry
            val profile = auth.currentUser
            val entry = LeaderboardEntry(
                uid = path,
                displayName = profile?.displayName ?: "User",
                score = (progress.totalQuizzes * 10) + (progress.correctAnswers * 5)
            )
            db.child("leaderboard").child(path).setValue(entry).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseRepository", "updateProgress failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun getProgressFlow(): Flow<LearningProgress> = callbackFlow {
        val db = database
        val path = userPath
        if (db == null || path == null) {
            trySend(LearningProgress())
            close()
            return@callbackFlow
        }

        val ref = db.child("users").child(path).child("progress")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val progress = snapshot.getValue(LearningProgress::class.java) ?: LearningProgress()
                trySend(progress)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseRepository", "getProgressFlow cancelled: ${error.message}")
                trySend(LearningProgress())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ─── Chat ─────────────────────────────────────────────────────────

    suspend fun saveChatMessage(message: ChatMessage): Result<Unit> {
        val db = database ?: return Result.success(Unit)
        val path = userPath ?: return Result.failure(Exception("User not logged in"))
        return try {
            db.child("users").child(path).child("chats").child(message.id).setValue(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseRepository", "saveChatMessage failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun getChatFlow(): Flow<List<ChatMessage>> = callbackFlow {
        val db = database
        val path = userPath
        if (db == null || path == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.child("users").child(path).child("chats")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(messages.sortedBy { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseRepository", "getChatFlow cancelled: ${error.message}")
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Delete all chat messages for the current user */
    suspend fun clearAllChatMessages(): Result<Unit> {
        val db = database ?: return Result.success(Unit)
        val path = userPath ?: return Result.success(Unit)
        return try {
            db.child("users").child(path).child("chats").removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseRepository", "clearAllChatMessages failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Auto-delete chat messages older than the given timestamp */
    suspend fun deleteOldChatMessages(olderThanTimestamp: Long): Result<Int> {
        val db = database ?: return Result.success(0)
        val path = userPath ?: return Result.success(0)
        return try {
            val snapshot = db.child("users").child(path).child("chats")
                .orderByChild("timestamp")
                .endAt(olderThanTimestamp.toDouble())
                .get()
                .await()

            var deleted = 0
            for (child in snapshot.children) {
                child.ref.removeValue().await()
                deleted++
            }

            if (deleted > 0) {
                Log.i("DatabaseRepository", "Auto-deleted $deleted old chat messages")
            }
            Result.success(deleted)
        } catch (e: Exception) {
            Log.e("DatabaseRepository", "deleteOldChatMessages failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ─── Roadmaps ─────────────────────────────────────────────────────

    suspend fun saveRoadmap(roadmap: LearningRoadmap): Result<Unit> {
        val db = database ?: return Result.success(Unit)
        val path = userPath ?: return Result.failure(Exception("User not logged in"))
        return try {
            db.child("users").child(path).child("roadmaps").child(roadmap.id).setValue(roadmap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRoadmapsFlow(): Flow<List<LearningRoadmap>> = callbackFlow {
        val db = database
        val path = userPath
        if (db == null || path == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.child("users").child(path).child("roadmaps")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(LearningRoadmap::class.java) }
                trySend(items.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ─── Leaderboard ──────────────────────────────────────────────────

    fun getLeaderboardFlow(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val db = database
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.child("leaderboard").orderByChild("score").limitToLast(50)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = snapshot.children.mapNotNull { it.getValue(LeaderboardEntry::class.java) }
                trySend(entries.reversed().mapIndexed { index, entry -> entry.copy(rank = index + 1) })
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ─── Daily Challenge ──────────────────────────────────────────────

    suspend fun getDailyChallenge(): Result<DailyChallenge> {
        val db = database ?: return Result.success(defaultChallenge())
        return try {
            val snapshot = db.child("daily_challenge").get().await()
            val challenge = snapshot.getValue(DailyChallenge::class.java) ?: defaultChallenge()
            Result.success(challenge)
        } catch (e: Exception) {
            Result.success(defaultChallenge())
        }
    }

    private fun defaultChallenge() = DailyChallenge(
        id = "default",
        title = "Reverse a String",
        description = "Write a function that reverses a given string without using built-in reverse functions.",
        difficulty = "Easy",
        baseCode = "fun reverseString(s: String): String {\n    // Your code here\n}"
    )
}
