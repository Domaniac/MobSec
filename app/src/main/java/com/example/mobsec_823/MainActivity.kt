package com.example.mobsec_823

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.User
import com.example.mobsec_823.ui.screens.DiscussionForumScreen
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
                    currentScreen = Screen.DiscussionForum
                }
            )
        }
        Screen.DiscussionForum -> {
            currentUser?.let { user ->
                DiscussionForumScreen(
                    user = user,
                    onBackClick = {
                        currentScreen = Screen.LoginTest
                        currentUser = null
                    }
                )
            }
        }
    }
}

sealed class Screen {
    object LoginTest : Screen()
    object DiscussionForum : Screen()
}
