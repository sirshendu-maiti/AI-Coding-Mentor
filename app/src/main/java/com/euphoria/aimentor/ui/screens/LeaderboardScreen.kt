package com.euphoria.aimentor.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.data.model.LeaderboardEntry
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard", color = MentorOnSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MentorOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MentorSurface)
            )
        },
        containerColor = MentorBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top 3 Header
            if (leaderboard.size >= 3) {
                TopThreeSection(leaderboard.take(3))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(leaderboard) { entry ->
                    LeaderboardItem(
                        entry = entry,
                        isCurrentUser = entry.uid == userProfile?.uid
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopThreeSection(topThree: List<LeaderboardEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MentorSurface)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        PodiumItem(topThree.getOrNull(1), 80.dp, Color(0xFFC0C0C0), "2")
        // 1st Place
        PodiumItem(topThree.getOrNull(0), 100.dp, Color(0xFFFFD700), "1")
        // 3rd Place
        PodiumItem(topThree.getOrNull(2), 70.dp, Color(0xFFCD7F32), "3")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodiumItem(entry: LeaderboardEntry?, size: androidx.compose.ui.unit.Dp, color: Color, rank: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (entry != null) {
            Box(contentAlignment = Alignment.TopCenter) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f))
                        .border(2.dp, color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.displayName.take(1).uppercase(),
                        color = color,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Badge(
                    containerColor = color,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(rank, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(entry.displayName, color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${entry.score} pts", color = color, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardItem(entry: LeaderboardEntry, isCurrentUser: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) MentorPrimary.copy(alpha = 0.1f) else MentorSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isCurrentUser) BorderStroke(1.dp, MentorPrimary.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "#${entry.rank}",
                color = if (entry.rank <= 3) Color(0xFFFFD700) else MentorOnSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MentorPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.displayName.take(1).uppercase(), color = MentorPrimary, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCurrentUser) "${entry.displayName} (You)" else entry.displayName,
                    color = MentorOnSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${entry.score} pts",
                color = MentorPrimary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
