package com.euphoria.aimentor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.data.model.ChatMessage
import com.euphoria.aimentor.data.model.MessageRole
import com.euphoria.aimentor.data.model.MessageType
import com.euphoria.aimentor.ui.components.MarkdownText
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.viewmodel.MainViewModel
import com.euphoria.aimentor.viewmodel.UiState

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    ChatContent(
        messages = messages,
        uiState = uiState,
        onSendMessage = { viewModel.sendChatMessage(it) },
        onClearChat = { viewModel.clearChat() }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatContent(
    messages: List<ChatMessage>,
    uiState: UiState,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Scroll to bottom when keyboard opens so user can see their context
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MentorBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MentorSurface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MentorPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("AI Mentor", color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(MentorSuccess))
                                Text("Online", color = MentorSuccess, fontSize = 11.sp)
                            }
                        }
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearChat) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = MentorOnSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item { WelcomeMessage() }
                }
                items(messages) { msg ->
                    MessageBubble(msg)
                }
                if (uiState.isLoading) {
                    item { TypingIndicator() }
                }
            }

            // Quick Prompts
            if (messages.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Explain recursion 🔄",
                        "What is Big O? ⏱️",
                        "Debug my code 🐛",
                        "Best practices 📖"
                    ).forEach { prompt ->
                        QuickPromptChip(prompt) {
                            onSendMessage(prompt)
                        }
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MentorSurface)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask anything about code...", color = MentorOnSurface.copy(alpha = 0.4f), fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MentorPrimary,
                        unfocusedBorderColor = MentorSurfaceVariant,
                        focusedContainerColor = MentorSurfaceVariant,
                        unfocusedContainerColor = MentorSurfaceVariant,
                        cursorColor = MentorPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(color = MentorOnSurface, fontSize = 14.sp),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4
                )
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank() && !uiState.isLoading) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (inputText.isNotBlank()) MentorPrimary else MentorSurfaceVariant,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun WelcomeMessage() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🤖", fontSize = 48.sp)
        Text("Hi! I'm your AI Coding Mentor ✨", color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Ask me anything about programming — from debugging to concepts to career advice!",
            color = MentorOnSurface.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
internal fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val isError = message.type == MessageType.ERROR

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isError) MentorError else MentorPrimary)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                val label = if (isError) "!" else "AI"
                Text(
                    label,
                    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        val bgColor = when {
            isError -> MentorError.copy(alpha = 0.15f)
            isUser -> MentorPrimary
            else -> MentorSurface
        }

        val bubbleShape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp
        )

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(bgColor)
                .then(
                    if (isError) Modifier.border(1.dp, MentorError.copy(alpha = 0.4f), bubbleShape)
                    else Modifier
                )
                .padding(12.dp)
        ) {
            when {
                isUser -> Text(message.content, color = Color.White, fontSize = 14.sp)
                isError -> Text(message.content, color = MentorError, fontSize = 14.sp)
                else -> MarkdownText(message.content)
            }
        }
    }
}

@Composable
internal fun TypingIndicator() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MentorPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MentorSurface)
                .padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = MentorPrimary, strokeWidth = 2.dp)
                Text("Thinking...", color = MentorOnSurface.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun QuickPromptChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MentorSurfaceVariant)
            .border(1.dp, MentorPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = MentorOnSurface.copy(alpha = 0.8f), fontSize = 13.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    AICodingMentorTheme {
        ChatContent(
            messages = listOf(
                ChatMessage(role = MessageRole.USER, content = "Hello!"),
                ChatMessage(role = MessageRole.ASSISTANT, content = "Hi! I'm your AI Coding Mentor. How can I help you today?"),
                ChatMessage(role = MessageRole.USER, content = "What is the difference between val and var in Kotlin?"),
                ChatMessage(role = MessageRole.ASSISTANT, content = "In Kotlin, `val` is used to declare read-only (immutable) variables, while `var` is used for mutable variables. For example:\n\n```kotlin\nval pi = 3.14 // Cannot be reassigned\nvar count = 0\ncount++ // Can be changed\n```")
            ),
            uiState = UiState(),
            onSendMessage = {},
            onClearChat = {}
        )
    }
}
