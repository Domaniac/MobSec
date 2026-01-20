package com.example.mobsec_823

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.User
import com.example.mobsec_823.ui.screens.ClassManagementScreen
import com.example.mobsec_823.ui.screens.DiscussionForumScreen
import com.example.mobsec_823.ui.screens.HomeMenuScreen
import com.example.mobsec_823.ui.screens.LoginTestScreen
import com.example.mobsec_823.ui.theme.MobSecTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize database helper with application context
        DatabaseHelper.initialize(applicationContext)

        enableEdgeToEdge()
        setContent {
            MobSecTheme {
                MobSecApp()
            }
        }
    }
}

@Composable
fun MobSecApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.LoginTest) }
    var currentUser by remember { mutableStateOf<User?>(null) }

    when (currentScreen) {
        Screen.LoginTest -> {
            LoginTestScreen(
                onUserSelected = { user ->
                    currentUser = user
                    currentScreen = Screen.HomeMenu
                }
            )
        }
        Screen.HomeMenu -> {
            currentUser?.let { user ->
                HomeMenuScreen(
                    user = user,
                    onNavigateToForum = {
                        currentScreen = Screen.DiscussionForum
                    },
                    onNavigateToClassManagement = {
                        currentScreen = Screen.ClassManagement
                    },
                    onLogout = {
                        currentScreen = Screen.LoginTest
                        currentUser = null
                    }
                )
            }
        }
        Screen.DiscussionForum -> {
            currentUser?.let { user ->
                DiscussionForumScreen(
                    user = user,
                    onBackClick = {
                        currentScreen = Screen.HomeMenu
                    }
                )
            }
        }
        Screen.ClassManagement -> {
            currentUser?.let { user ->
                ClassManagementScreen(
                    user = user,
                    onBackClick = {
                        currentScreen = Screen.HomeMenu
                    }
                )
            }
        }
    }
}

sealed class Screen {
    object LoginTest : Screen()
    object HomeMenu : Screen()
    object DiscussionForum : Screen()
    object ClassManagement : Screen()
}
