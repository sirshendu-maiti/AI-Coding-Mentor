package com.euphoria.aimentor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.euphoria.aimentor.data.model.*
import com.euphoria.aimentor.data.repository.AIRepository
import com.euphoria.aimentor.data.repository.AuthRepository
import com.euphoria.aimentor.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class MainViewModel : ViewModel() {

    private val repository = AIRepository()
    private val authRepository = AuthRepository()

    // FIX: DatabaseRepository is lazy to avoid initialization crash on startup
    private val databaseRepository by lazy { DatabaseRepository() }

    // ─── Auth State ───────────────────────────────────────────────────
    private val _isUserLoggedIn = MutableStateFlow(authRepository.isLoggedIn)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(authRepository.getUserProfile())
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // ─── Sync State ───────────────────────────────────────────────────
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // ─── Code Editor State ────────────────────────────────────────────
    private val _codeInput = MutableStateFlow("")
    val codeInput: StateFlow<String> = _codeInput.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(ProgrammingLanguage.AUTO)
    val selectedLanguage: StateFlow<ProgrammingLanguage> = _selectedLanguage.asStateFlow()

    private val _analysisResult = MutableStateFlow<String?>(null)
    val analysisResult: StateFlow<String?> = _analysisResult.asStateFlow()

    // ─── Chat State ───────────────────────────────────────────────────
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // FIX: Track whether user is actively chatting to prevent Firebase sync overwrites
    private var isChatActive = false

    // ─── Quiz State ───────────────────────────────────────────────────
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedAnswers: StateFlow<Map<String, String>> = _selectedAnswers.asStateFlow()

    // ─── Roadmap State ────────────────────────────────────────────────
    private val _learningRoadmap = MutableStateFlow<LearningRoadmap?>(null)
    val learningRoadmap: StateFlow<LearningRoadmap?> = _learningRoadmap.asStateFlow()

    // ─── Challenge & Leaderboard ──────────────────────────────────────
    private val _dailyChallenge = MutableStateFlow<DailyChallenge?>(null)
    val dailyChallenge: StateFlow<DailyChallenge?> = _dailyChallenge.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    // ─── Learning Progress ────────────────────────────────────────────
    private val _progress = MutableStateFlow(LearningProgress())
    val progress: StateFlow<LearningProgress> = _progress.asStateFlow()

    // ─── History ──────────────────────────────────────────────────────
    private val _history = MutableStateFlow<List<CodeHistoryItem>>(emptyList())
    val history: StateFlow<List<CodeHistoryItem>> = _history.asStateFlow()

    // ─── UI State ─────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _beginnerMode = MutableStateFlow(false)
    val beginnerMode: StateFlow<Boolean> = _beginnerMode.asStateFlow()

    init {
        if (_isUserLoggedIn.value) {
            observeRealtimeData()
            loadDailyChallenge()
            autoCleanupOldChats()
        }
    }

    private fun observeRealtimeData() {
        viewModelScope.launch {
            _isSyncing.value = true
            databaseRepository.getHistoryFlow()
                .catch {
                    _isSyncing.value = false
                }
                .collectLatest { list ->
                    _history.value = list
                    _isSyncing.value = false
                }
        }
        viewModelScope.launch {
            databaseRepository.getProgressFlow()
                .catch { /* ignore */ }
                .collectLatest { prog -> _progress.value = prog }
        }
        viewModelScope.launch {
            databaseRepository.getChatFlow()
                .catch { /* ignore */ }
                .collectLatest { messages ->
                    // FIX: Only sync from Firebase if user is NOT actively chatting
                    // This prevents Firebase overwrites during rapid send/receive cycles
                    if (!isChatActive && messages.isNotEmpty()) {
                        _chatMessages.value = messages
                    }
                }
        }
        viewModelScope.launch {
            databaseRepository.getLeaderboardFlow()
                .catch { /* ignore */ }
                .collectLatest { entries -> _leaderboard.value = entries }
        }
    }

    private fun loadDailyChallenge() {
        viewModelScope.launch {
            databaseRepository.getDailyChallenge().onSuccess { challenge ->
                _dailyChallenge.value = challenge
            }
        }
    }

    // ─── Auto-cleanup old chat messages (older than 7 days) ───────────
    private fun autoCleanupOldChats() {
        viewModelScope.launch {
            try {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                databaseRepository.deleteOldChatMessages(sevenDaysAgo)
            } catch (_: Exception) {
                // Silent — cleanup is best-effort
            }
        }
    }

    // ─── Auth Actions ─────────────────────────────────────────────────

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please fill all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            authRepository.signInWithEmail(email, password).fold(
                onSuccess = { onAuthSuccess() },
                onFailure = { e ->
                    _uiState.value = UiState(error = "Login failed: ${e.message}")
                }
            )
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please fill all fields")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            authRepository.registerWithEmail(email, password).fold(
                onSuccess = { onAuthSuccess() },
                onFailure = { e ->
                    _uiState.value = UiState(error = "Registration failed: ${e.message}")
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { onAuthSuccess() },
                onFailure = { e ->
                    _uiState.value = UiState(error = "Google Login failed: ${e.message}")
                }
            )
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            authRepository.signInAnonymously().fold(
                onSuccess = { onAuthSuccess() },
                onFailure = { e ->
                    _uiState.value = UiState(error = "Guest login failed: ${e.message}")
                }
            )
        }
    }

    private fun onAuthSuccess() {
        _isUserLoggedIn.value = true
        _userProfile.value = authRepository.getUserProfile()
        observeRealtimeData()
        loadDailyChallenge()
        autoCleanupOldChats()
        _uiState.value = UiState()
    }

    fun updateProfile(name: String) {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            authRepository.updateDisplayName(name).fold(
                onSuccess = {
                    _userProfile.value = authRepository.getUserProfile()
                    _uiState.value = UiState(successMessage = "Profile updated successfully")
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = "Update failed: ${e.message}")
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isUserLoggedIn.value = false
        _userProfile.value = null
        _history.value = emptyList()
        _chatMessages.value = emptyList()
        _progress.value = LearningProgress()
        _learningRoadmap.value = null
        _leaderboard.value = emptyList()
        _dailyChallenge.value = null
        _analysisResult.value = null
        _quizQuestions.value = emptyList()
        isChatActive = false
    }

    // ─── Password Reset ───────────────────────────────────────────────

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your email")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            authRepository.sendPasswordResetEmail(email).fold(
                onSuccess = {
                    _uiState.value = UiState(successMessage = "Password reset email sent to $email")
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = "Reset failed: ${e.message}")
                }
            )
        }
    }

    // ─── Code Editor Actions ──────────────────────────────────────────

    fun startDailyChallenge() {
        _dailyChallenge.value?.let { challenge ->
            _codeInput.value = challenge.baseCode
            _selectedLanguage.value = ProgrammingLanguage.KOTLIN
        }
    }

    fun updateCode(code: String) {
        _codeInput.value = code
        if (_selectedLanguage.value == ProgrammingLanguage.AUTO && code.length > 10) {
            _selectedLanguage.value = ProgrammingLanguage.fromCode(code)
        }
    }

    fun selectLanguage(lang: ProgrammingLanguage) {
        _selectedLanguage.value = lang
    }

    fun setBeginnerMode(on: Boolean) {
        _beginnerMode.value = on
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun analyzeCode() {
        val code = _codeInput.value.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter some code first!")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            val lang = _selectedLanguage.value.displayName
            repository.analyzeCode(code, lang).fold(
                onSuccess = { result ->
                    _analysisResult.value = result
                    addToHistory(code, lang, "Debug analysis", result.contains("error", ignoreCase = true))
                    _uiState.value = UiState()
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = e.message ?: "Analysis failed")
                }
            )
        }
    }

    fun explainCode() {
        val code = _codeInput.value.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter some code first!")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            val lang = _selectedLanguage.value.displayName
            repository.explainCode(code, lang, _beginnerMode.value).fold(
                onSuccess = { result ->
                    _analysisResult.value = result
                    _uiState.value = UiState()
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = e.message ?: "Explain failed")
                }
            )
        }
    }

    fun analyzeComplexity() {
        val code = _codeInput.value.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter some code first!")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            repository.analyzeComplexity(code, _selectedLanguage.value.displayName).fold(
                onSuccess = { result ->
                    _analysisResult.value = result
                    _uiState.value = UiState()
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = e.message ?: "Complexity analysis failed")
                }
            )
        }
    }

    fun generateTestCases() {
        val code = _codeInput.value.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter some code first!")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            repository.generateTestCases(code, _selectedLanguage.value.displayName).fold(
                onSuccess = { result ->
                    _analysisResult.value = result
                    _uiState.value = UiState()
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = e.message ?: "Test generation failed")
                }
            )
        }
    }

    fun generateInterviewQuestions() {
        val code = _codeInput.value.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter some code first!")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            repository.generateInterviewQuestions(code, _selectedLanguage.value.displayName).fold(
                onSuccess = { result ->
                    _analysisResult.value = result
                    _uiState.value = UiState()
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = e.message ?: "Interview Q generation failed")
                }
            )
        }
    }

    // ─── Quiz (server returns pre-parsed objects) ──────────────────────

    fun generateQuiz() {
        val code = _codeInput.value.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter some code first!")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            repository.generateQuiz(code, _selectedLanguage.value.displayName).fold(
                onSuccess = { questions ->
                    _quizQuestions.value = questions
                    _selectedAnswers.value = emptyMap()
                    _quizScore.value = 0
                    _uiState.value = UiState()
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = e.message ?: "Quiz generation failed")
                }
            )
        }
    }

    // ─── Roadmap (server returns pre-parsed objects) ───────────────────

    fun generateRoadmap(topic: String) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            val level = if (_beginnerMode.value) "Beginner" else "Advanced"
            repository.generateRoadmap(topic, level).fold(
                onSuccess = { roadmap ->
                    _learningRoadmap.value = roadmap
                    _uiState.value = UiState()
                },
                onFailure = { e ->
                    _uiState.value = UiState(error = e.message ?: "Roadmap generation failed")
                }
            )
        }
    }

    // ─── Chat Actions (FIXED) ─────────────────────────────────────────

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return

        // Mark chat as active to prevent Firebase sync overwrites
        isChatActive = true

        // Add user message to local state immediately for instant UI feedback
        val userMsg = ChatMessage(role = MessageRole.USER, content = message)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            // Save to Firebase in background (non-blocking)
            databaseRepository.saveChatMessage(userMsg)

            _uiState.value = UiState(isLoading = true)

            // Send all prior messages (excluding the one we just added) as history
            val history = _chatMessages.value.dropLast(1)
            repository.chat(message, history, _codeInput.value.ifBlank { null }).fold(
                onSuccess = { reply ->
                    val aiMsg = ChatMessage(role = MessageRole.ASSISTANT, content = reply)
                    _chatMessages.value = _chatMessages.value + aiMsg
                    databaseRepository.saveChatMessage(aiMsg)
                    _uiState.value = UiState()
                },
                onFailure = { e ->
                    // FIX: Show error as a message bubble in the chat UI
                    // so the user sees what went wrong inline (not just a toast)
                    val errorMsg = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "⚠️ ${e.message ?: "Something went wrong. Please try again."}",
                        type = MessageType.ERROR
                    )
                    _chatMessages.value = _chatMessages.value + errorMsg
                    _uiState.value = UiState()
                }
            )

            // Allow Firebase sync again after a short delay
            kotlinx.coroutines.delay(2000)
            isChatActive = false
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            databaseRepository.clearAllChatMessages()
        }
    }

    fun selectQuizAnswer(questionId: String, label: String) {
        _selectedAnswers.value = _selectedAnswers.value + (questionId to label)

        val currentQuestions = _quizQuestions.value
        val currentSelected = _selectedAnswers.value

        val score = currentQuestions.count { q ->
            val selected = currentSelected[q.id] ?: return@count false
            q.options.any { it.label == selected && it.isCorrect }
        }
        _quizScore.value = score

        if (currentSelected.size == currentQuestions.size && currentQuestions.isNotEmpty()) {
            val newProgress = _progress.value.copy(
                totalQuizzes = _progress.value.totalQuizzes + 1,
                correctAnswers = _progress.value.correctAnswers + score
            )
            viewModelScope.launch {
                databaseRepository.updateProgress(newProgress)
            }
        }
    }

    fun setCodeFromOcr(text: String) {
        _codeInput.value = text
        _selectedLanguage.value = ProgrammingLanguage.fromCode(text)
    }

    private fun addToHistory(code: String, language: String, summary: String, hasErrors: Boolean) {
        val item = CodeHistoryItem(
            code = code.take(500),
            language = language,
            summary = summary,
            hasErrors = hasErrors
        )
        viewModelScope.launch {
            databaseRepository.saveHistoryItem(item)
        }
    }

    fun loadHistoryItem(item: CodeHistoryItem) {
        _codeInput.value = item.code
    }

    fun clearAnalysis() {
        _analysisResult.value = null
    }
}
