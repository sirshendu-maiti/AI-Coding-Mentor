package com.euphoria.aimentor.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.euphoria.aimentor.ui.screens.*
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Auth        : Screen("auth",        "Auth",        Icons.Default.Lock)
    object Home        : Screen("home",        "Home",        Icons.Default.Home)
    object Editor      : Screen("editor",      "Editor",      Icons.Default.Code)
    object Chat        : Screen("chat",        "Chat",        Icons.Default.Chat)
    object Learn       : Screen("learn",       "Learn",       Icons.Default.School)
    object History     : Screen("history",     "History",     Icons.Default.History)
    object Profile     : Screen("profile",     "Profile",     Icons.Default.Person)
    object Leaderboard : Screen("leaderboard", "Leaderboard", Icons.Default.EmojiEvents)
}

val bottomNavItems = listOf(Screen.Home, Screen.Editor, Screen.Chat, Screen.Learn, Screen.History)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MentorApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val history by viewModel.history.collectAsState()
    val progress by viewModel.progress.collectAsState()

    AICodingMentorTheme {
        if (!isLoggedIn) {
            AuthScreen(viewModel) {
                // Success handled by state observation
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MentorSurface,
                        contentColor = MentorPrimary,
                        tonalElevation = 0.dp
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = { 
                                    BadgedBox(
                                        badge = {
                                            if (screen == Screen.History && history.isNotEmpty()) {
                                                Badge { Text(history.size.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                },
                                label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MentorPrimary,
                                    selectedTextColor = MentorPrimary,
                                    unselectedIconColor = MentorOnSurface.copy(alpha = 0.4f),
                                    unselectedTextColor = MentorOnSurface.copy(alpha = 0.4f),
                                    indicatorColor = MentorPrimary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        enterTransition = {
                            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                progress = progress,
                                historyCount = history.size,
                                onNavigateToEditor = { navController.navigate(Screen.Editor.route) },
                                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                                onNavigateToLearn = { navController.navigate(Screen.Learn.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                                onSignOut = { viewModel.signOut() },
                                viewModel = viewModel
                            )
                        }
                        composable(Screen.Editor.route) {
                            CodeEditorScreen(viewModel)
                        }
                        composable(Screen.Chat.route) {
                            ChatScreen(viewModel)
                        }
                        composable(Screen.Learn.route) {
                            LearnScreen(viewModel)
                        }
                        composable(Screen.History.route) {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { navController.navigate(Screen.Editor.route) }
                            )
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Leaderboard.route) {
                            LeaderboardScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Sync Indicator Overlay
                    SyncIndicator(isSyncing)
                }
            }
        }
    }
}

@Composable
fun SyncIndicator(isSyncing: Boolean) {
    AnimatedVisibility(
        visible = isSyncing,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                color = MentorSurface.copy(alpha = 0.9f),
                shape = CircleShape,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MentorPrimary
                    )
                    Text(
                        "Syncing with Cloud...",
                        color = MentorOnSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
