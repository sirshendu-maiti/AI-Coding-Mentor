package com.euphoria.aimentor.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.data.model.QuizQuestion
import com.euphoria.aimentor.data.model.RoadmapStep
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.viewmodel.MainViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LearnScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Quizzes", "Roadmaps")

    Column(modifier = Modifier.fillMaxSize().background(MentorBackground)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MentorSurface,
            contentColor = MentorPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MentorPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Crossfade(targetState = selectedTab, label = "LearnContent") { tab ->
            when (tab) {
                0 -> QuizTab(viewModel)
                1 -> RoadmapTab(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizTab(viewModel: MainViewModel) {
    val quizQuestions by viewModel.quizQuestions.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()
    val quizScore by viewModel.quizScore.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val codeInput by viewModel.codeInput.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("AI Quiz Generator", color = MentorOnSurface, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("Test your knowledge on the current code", color = MentorOnSurface.copy(alpha = 0.5f), fontSize = 13.sp)
        }

        if (codeInput.isBlank()) {
            item { EmptyCodeWarning() }
        } else {
            item {
                Button(
                    onClick = { viewModel.generateQuiz() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MentorPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Quiz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (quizQuestions.isNotEmpty()) {
            item {
                ScoreCard(score = quizScore, total = quizQuestions.size)
            }
            items(quizQuestions) { question ->
                QuizQuestionCard(
                    question = question,
                    selectedAnswer = selectedAnswers[question.id],
                    onAnswerSelected = { label -> viewModel.selectQuizAnswer(question.id, label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RoadmapTab(viewModel: MainViewModel) {
    val roadmap by viewModel.learningRoadmap.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var topic by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("Learning Roadmap", color = MentorOnSurface, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("Generate a step-by-step path for any topic", color = MentorOnSurface.copy(alpha = 0.5f), fontSize = 13.sp)
        }

        item {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Jetpack Compose, Data Structures...") },
                label = { Text("What do you want to learn?") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MentorPrimary,
                    unfocusedBorderColor = MentorSurfaceVariant,
                    focusedTextColor = MentorOnSurface,
                    unfocusedTextColor = MentorOnSurface
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.generateRoadmap(topic) },
                        enabled = topic.isNotBlank() && !uiState.isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Generate", tint = MentorPrimary)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MentorPrimary)
                }
            }
        }

        roadmap?.let {
            item {
                Text(
                    "${it.level} path for ${it.topic}",
                    color = MentorPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            items(it.steps) { step ->
                RoadmapStepItem(step)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RoadmapStepItem(step: RoadmapStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MentorPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(MentorPrimary.copy(alpha = 0.3f))
            )
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MentorSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(step.title, color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(step.description, color = MentorOnSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                if (step.resources.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        step.resources.forEach { res ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MentorPrimary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(res, color = MentorPrimary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCodeWarning() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MentorWarning.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MentorWarning)
            Text("Paste code in the Editor first to generate a quiz!", color = MentorOnSurface.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ScoreCard(score: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MentorSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("${(score * 100 / (total.takeIf { it > 0 } ?: 1))}%", color = MentorPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Column {
                Text("Quiz Results", color = MentorOnSurface, fontWeight = FontWeight.Bold)
                Text("$score/$total correct answers", color = MentorOnSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizQuestionCard(
    question: QuizQuestion,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit
) {
    val isAnswered = selectedAnswer != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MentorSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(question.question, color = MentorOnSurface, fontWeight = FontWeight.SemiBold)
            question.options.forEach { option ->
                val isSelected = selectedAnswer == option.label
                val color = when {
                    !isAnswered -> if (isSelected) MentorPrimary else MentorSurfaceVariant
                    option.isCorrect -> MentorSuccess
                    isSelected -> MentorError
                    else -> MentorSurfaceVariant
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f))
                        .border(1.dp, color, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isAnswered) { onAnswerSelected(option.label) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option.label, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    Text(option.text, color = MentorOnSurface, fontSize = 14.sp)
                }
            }
            if (isAnswered) {
                Text(question.explanation, color = MentorOnSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}
