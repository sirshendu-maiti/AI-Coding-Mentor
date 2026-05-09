package com.euphoria.aimentor.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.data.model.DailyChallenge
import com.euphoria.aimentor.data.model.LearningProgress
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    progress: LearningProgress,
    historyCount: Int,
    onNavigateToEditor: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToLearn: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: MainViewModel
) {
    val scrollState = rememberScrollState()
    val dailyChallenge by viewModel.dailyChallenge.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentorBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ─── Header ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "AI Coding Mentor",
                    color = MentorOnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Your personal coding tutor 🚀",
                    color = MentorOnSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onNavigateToLeaderboard) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard", tint = Color(0xFFFFD700))
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MentorPrimary)
                        .clickable(onClick = onNavigateToProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // ─── Daily Challenge Widget ───────────────────────────────────
        dailyChallenge?.let { challenge ->
            DailyChallengeWidget(challenge) {
                viewModel.startDailyChallenge()
                onNavigateToEditor()
            }
        }

        // ─── Hero Banner ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(MentorPrimary, Color(0xFF9B59B6))
                    )
                )
                .clickable(onClick = onNavigateToEditor)
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📷 Screenshot → Solution", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Snap a photo of your code\nand get instant AI help", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Try Now →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
            }
        }

        // ─── Stats Row ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard( "${progress.totalQuizzes}", "Quizzes", Modifier.weight(1f))
            StatCard( "${progress.correctAnswers}", "Correct", Modifier.weight(1f))
            StatCard( "$historyCount", "Sessions", Modifier.weight(1f))
        }

        // ─── Quick Actions ────────────────────────────────────────────
        Text("Quick Actions", color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    icon = Icons.Default.Code,
                    title = "Code Editor",
                    subtitle = "Analyze & debug code",
                    color = MentorPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToEditor
                )
                ActionCard(
                    icon = Icons.Default.Chat,
                    title = "AI Chat",
                    subtitle = "Ask anything",
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToChat
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    icon = Icons.Default.School,
                    title = "Learn Mode",
                    subtitle = "Quizzes & explanations",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToLearn
                )
                ActionCard(
                    icon = Icons.Default.History,
                    title = "History",
                    subtitle = "$historyCount past sessions",
                    color = Color(0xFFFFA726),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToHistory
                )
            }
        }

        // ─── Supported Languages ──────────────────────────────────────
        Text("Supported Languages", color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("🐍 Python", "☕ Java", "🟨 JS", "🇰 Kotlin", "⚙️ C/C++").forEach { lang ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MentorSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(lang, color = MentorOnSurface.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // Sign Out Button at bottom
        TextButton(
            onClick = onSignOut,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Sign Out", color = MentorError)
        }
    }
}

@Composable
fun DailyChallengeWidget(challenge: DailyChallenge, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MentorSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MentorPrimary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFFFD700))
                Text("Daily Challenge", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MentorPrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(challenge.difficulty, color = MentorPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Text(challenge.title, color = MentorOnSurface, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(challenge.description, color = MentorOnSurface.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 2)
            
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MentorPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Challenge", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MentorSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, color = MentorPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, color = MentorOnSurface.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MentorSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Text(title, color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = MentorOnSurface.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}
