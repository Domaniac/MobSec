package com.example.mobsec_823

import com.example.mobsec_823.ui.screens.GroupManagementScreen
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
import com.example.mobsec_823.ui.screens.StudentDashboardScreen
import com.example.mobsec_823.ui.screens.StudentQueryScreen
import com.example.mobsec_823.ui.screens.TeacherDashboardScreen
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
                    // FIX: Redirect based on role
                    currentScreen = if (user.role == "teacher") {
                        Screen.TeacherDashboard
                    } else {
                        Screen.HomeMenu
                    }
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
                    onNavigateToStudentDashboard = {
                        currentScreen = Screen.StudentDashboard
                    },
                    onNavigateToClassManagement = {
                        currentScreen = Screen.ClassManagement
                    },

                    onNavigateToStudentQuery = {
                        currentScreen = Screen.StudentQuery
                    },
                    onNavigateToTeacherDashboard = {
                        currentScreen = Screen.TeacherDashboard
                    },
                    onNavigateToGroupManagement = {
                        currentScreen = Screen.GroupManagement
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
        Screen.StudentDashboard -> {
            currentUser?.let { user ->
                StudentDashboardScreen(
                    user = user,
                    onBackClick = { currentScreen = Screen.HomeMenu }
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
                    onSubmitQuery = { teacherId, questionText, priority ->
                        // FIX: Use toIntOrNull() and provide a default value (0)
                        // to prevent NumberFormatException if classId is null or empty
                        val safeClassId = user.classId?.toIntOrNull() ?: 0

                        DatabaseHelper.createQuestion(
                            studentId = user.userId,
                            classId = safeClassId,
                            teacherId = teacherId,
                            question = questionText,
                            priority = priority
                        )
                    }
                )
            }
        }
        Screen.TeacherDashboard -> {
            currentUser?.let { user ->
                TeacherDashboardScreen(
                    teacher = user,
                    onLogout = {
                        currentScreen = Screen.LoginTest
                        currentUser = null
                    }
                )
            }
        }

        Screen.GroupManagement -> {
            currentUser?.let { user ->
                val safeClassId = user.classId?.toIntOrNull() ?: 1

                GroupManagementScreen(
                    classId = safeClassId,
                    user = user, // Changed parameter name to match the User class requirement
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
    object ClassList : Screen()
    object DiscussionForum : Screen()
    object ClassManagement : Screen()
    object StudentDashboard : Screen()
    object StudentQuery : Screen()
    object TeacherDashboard : Screen()
    object GroupManagement : Screen()
}
