package com.euphoria.aimentor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.data.model.CodeHistoryItem
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: MainViewModel, onNavigateToEditor: () -> Unit) {
    val history by viewModel.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MentorBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.History, contentDescription = null, tint = MentorPrimary, modifier = Modifier.size(24.dp))
            Column {
                Text("History", color = MentorOnSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("${history.size} past sessions", color = MentorOnSurface.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("📂", fontSize = 64.sp)
                    Text("No history yet", color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Analyze some code to see it here", color = MentorOnSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                    Button(
                        onClick = onNavigateToEditor,
                        colors = ButtonDefaults.buttonColors(containerColor = MentorPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go to Code Editor")
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { item ->
                    HistoryCard(item) {
                        viewModel.loadHistoryItem(item)
                        onNavigateToEditor()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: CodeHistoryItem, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MentorSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Language badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MentorPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.language.take(2).uppercase(),
                    color = MentorPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.language, color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        dateFormat.format(Date(item.timestamp)),
                        color = MentorOnSurface.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }

                // Code preview
                Text(
                    item.code.take(80).replace("\n", " "),
                    color = MentorCodeText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.hasErrors) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MentorError.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Has Errors", color = MentorError, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MentorSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Clean", color = MentorSuccess, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Text("Tap to reload", color = MentorOnSurface.copy(alpha = 0.3f), fontSize = 10.sp)
                }
            }
        }
    }
}
