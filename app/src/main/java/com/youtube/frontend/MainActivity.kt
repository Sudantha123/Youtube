package com.youtube.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.youtube.frontend.ui.screens.*
import com.youtube.frontend.ui.theme.YouTubeFrontendTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Shorts : Screen("shorts", "Shorts", Icons.Default.VideoLibrary)
    object Subscriptions : Screen("subscriptions", "Subscriptions", Icons.Default.Subscriptions)
    object Library : Screen("library", "You", Icons.Default.Person)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Player : Screen("player/{videoId}", "Player", Icons.Default.PlayArrow) {
        fun createRoute(videoId: String) = "player/$videoId"
    }
    object Channel : Screen("channel/{channelId}", "Channel", Icons.Default.AccountCircle) {
        fun createRoute(channelId: String) = "channel/$channelId"
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YouTubeFrontendTheme {
                YouTubeAppNav()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeAppNav() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Shorts,
        Screen.Subscriptions,
        Screen.Library
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            // Hide bottom bar on player and search and channel
            val showBottomBar = currentDestination?.route?.let { route ->
                bottomNavItems.any { it.route == route }
            } ?: false || currentDestination?.route == "home"

            if (showBottomBar || currentDestination?.hierarchy?.any { it.route == "home" } == true) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }
            composable(Screen.Shorts.route) {
                ShortsScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    }
                )
            }
            composable(Screen.Subscriptions.route) {
                SubscriptionsScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                VideoPlayerScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() },
                    onVideoClick = { newVideoId ->
                        navController.navigate(Screen.Player.createRoute(newVideoId))
                    },
                    onChannelClick = { channelId ->
                        navController.navigate(Screen.Channel.createRoute(channelId))
                    }
                )
            }
            composable(
                route = Screen.Channel.route,
                arguments = listOf(navArgument("channelId") { type = NavType.StringType })
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                ChannelScreen(
                    channelId = channelId,
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
