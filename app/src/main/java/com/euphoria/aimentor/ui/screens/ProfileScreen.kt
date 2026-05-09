package com.euphoria.aimentor.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var isEditingName by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(userProfile?.displayName ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = MentorOnSurface) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MentorPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile?.displayName?.take(1)?.uppercase() ?: "?",
                    color = MentorPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // User Info
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditingName) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MentorOnSurface,
                                unfocusedTextColor = MentorOnSurface
                            ),
                            singleLine = true,
                            modifier = Modifier.width(200.dp)
                        )
                        IconButton(onClick = {
                            viewModel.updateProfile(newName)
                            isEditingName = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = MentorSuccess)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = userProfile?.displayName ?: "User",
                            color = MentorOnSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MentorPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                Text(
                    text = userProfile?.email ?: "Guest Mode",
                    color = MentorOnSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            // Stats Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MentorSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Learning Progress", color = MentorOnSurface, fontWeight = FontWeight.Bold)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ProfileStatItem("Quizzes", progress.totalQuizzes.toString(), Icons.Default.Quiz)
                        ProfileStatItem("Accuracy", "${if (progress.totalQuizzes > 0) (progress.correctAnswers * 100 / (progress.totalQuizzes * 3)) else 0}%", Icons.Default.Timeline)
                        ProfileStatItem("Streak", "${progress.streak} days", Icons.Default.Whatshot)
                    }
                }
            }

            // Account Actions
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MentorError.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = MentorError)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = MentorError)
                }
                
                val joinedDate = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(userProfile?.createdAt ?: System.currentTimeMillis()))
                Text(
                    text = "Joined $joinedDate",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MentorOnSurface.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ProfileStatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = MentorPrimary, modifier = Modifier.size(24.dp))
        Text(value, color = MentorOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = MentorOnSurface.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}
