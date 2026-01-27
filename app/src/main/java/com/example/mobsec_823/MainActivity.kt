package com.example.mobsec_823

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.mobsec_823.data.ClassEntity
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.User
import com.example.mobsec_823.ui.screens.ClassListScreen
import com.example.mobsec_823.ui.screens.ClassManagementScreen
import com.example.mobsec_823.ui.screens.DiscussionForumScreen
import com.example.mobsec_823.ui.screens.HomeMenuScreen
import com.example.mobsec_823.ui.screens.LoginTestScreen
import com.example.mobsec_823.ui.screens.StudentQueryScreen
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
    var selectedClass by remember { mutableStateOf<ClassEntity?>(null) }

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
                        currentScreen = Screen.ClassList
                    },
                    onNavigateToClassManagement = {
                        currentScreen = Screen.ClassManagement
                    },
                    onNavigateToStudentQuery = {
                        currentScreen = Screen.StudentQuery
                    },
                    onLogout = {
                        currentScreen = Screen.LoginTest
                        currentUser = null
                        selectedClass = null
                    }
                )
            }
        }
        Screen.ClassList -> {
            currentUser?.let { user ->
                ClassListScreen(
                    user = user,
                    onClassSelected = { classEntity ->
                        selectedClass = classEntity
                        currentScreen = Screen.DiscussionForum
                    },
                    onBackClick = {
                        currentScreen = Screen.HomeMenu
                    }
                )
            }
        }
        Screen.DiscussionForum -> {
            currentUser?.let { user ->
                selectedClass?.let { classEntity ->
                    DiscussionForumScreen(
                        user = user,
                        classId = classEntity.classId,
                        className = classEntity.className,
                        onBackClick = {
                            currentScreen = Screen.ClassList
                        }
                    )
                }
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
        Screen.StudentQuery -> {
            currentUser?.let { user ->
                StudentQueryScreen(
                    user = user,
                    onBackClick = {
                        currentScreen = Screen.HomeMenu
                    },
                    onSubmitQuery = { questionText, priority ->
                        DatabaseHelper.createQuestion(
                            studentId = user.userId,
                            classId = user.classId,
                            teacherId = 1, // or from user if available
                            questionText = questionText,
                            priority = priority
                        )
                    }
                )
            }
        }

    }
}

sealed class Screen {
    object LoginTest : Screen()
    object HomeMenu : Screen()
    object ClassList : Screen()
    object DiscussionForum : Screen()
    object ClassManagement : Screen()
    object StudentQuery : Screen()
}
