package com.example.mobsec_823.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobsec_823.data.*
import com.example.mobsec_823.data.DatabaseHelper
import kotlinx.coroutines.launch

// ============== SPACING & SIZING CONSTANTS ==============
private val CardSpacing = 12.dp
private val CardInnerPadding = 16.dp
private val CardHeaderBottomPadding = 10.dp
private val CardIconSize = 22.dp
private val CardChevronSize = 20.dp
private val ListItemVerticalPadding = 4.dp
private val SectionBottomPadding = 16.dp
private val BulletIconSize = 8.dp
private val ForumIconSize = 16.dp
private val HeaderFontSize = 16.sp     // titleMedium equivalent
private val BodyFontSize = 14.sp       // bodyMedium equivalent
private val DetailFontSize = 12.sp     // bodySmall / labelSmall equivalent
private val StatValueFontSize = 22.sp  // titleLarge equivalent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuScreen(
    user: User,
    onNavigateToProfile: () -> Unit,
    onNavigateToForum: () -> Unit,
    onNavigateToClassManagement: () -> Unit,
    onNavigateToStudentQuery: () -> Unit,
    onNavigateToStudentDashboard: () -> Unit,
    onNavigateToTeacherDashboard: () -> Unit,
    onNavigateToGroupManagement: () -> Unit,
    onNavigateToLocationSharing: () -> Unit,
    onNavigateToResourceLibrary: () -> Unit,
    onNavigateToAdminTeacherManagement: () -> Unit = {},
    onMenuClick: () -> Unit
) {
    val isAdmin = user.role.equals("Admin", ignoreCase = true)
    val isTeacher = user.role.equals("Teacher", ignoreCase = true)
    val isStudent = user.role.equals("Student", ignoreCase = true)
    val isParent = user.role.equals("Parent", ignoreCase = true)

    // ---------- Shared state ----------
    var userClasses by remember { mutableStateOf<List<ClassEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // ---------- Teacher state ----------
    var unansweredQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }

    // ---------- Student state ----------
    var studentQuestions by remember { mutableStateOf<List<Question>>(emptyList()) }

    // ---------- Admin state ----------
    var allClasses by remember { mutableStateOf<List<ClassEntity>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    // ---------- Recent forum posts (all roles) ----------
    var recentPosts by remember { mutableStateOf<List<ForumPost>>(emptyList()) }

    val scope = rememberCoroutineScope()

    val refreshData = {
        scope.launch {
            isLoading = true
            try {
                // All roles: get user classes
                userClasses = DatabaseHelper.getUserClasses(user.userId)

                // Fetch recent forum posts from the user's classes (up to 3 latest)
                val allPosts = mutableListOf<ForumPost>()
                for (cls in userClasses.take(5)) {
                    val posts = DatabaseHelper.getClassPosts(cls.classId)
                    allPosts.addAll(posts)
                }
                recentPosts = allPosts.sortedByDescending { it.createdAt }.take(3)

                // Role-specific data
                when {
                    isTeacher -> {
                        val questions = DatabaseHelper.getQuestionsForTeacher(user.userId)
                        unansweredQuestions = questions.filter {
                            it.status.equals("open", ignoreCase = true) || it.answer.isNullOrBlank()
                        }
                    }
                    isStudent -> {
                        studentQuestions = DatabaseHelper.getQuestionsByStudent(user.userId)
                    }
                    isAdmin -> {
                        allClasses = DatabaseHelper.getAllClasses()
                        allUsers = DatabaseHelper.getAllUsers()
                    }
                }
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    // Load data on first composition or when user changes
    LaunchedEffect(user.userId) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MobSec Home") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Welcome header
            Text(
                text = "Welcome, ${user.fullName ?: user.username}!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = user.role,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = SectionBottomPadding)
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            } else {

                // ===================== TEACHER OVERVIEW =====================
                if (isTeacher) {
                    // Unanswered Questions Card
                    OverviewCard(
                        title = "Unanswered Questions",
                        icon = Icons.Default.QuestionAnswer,
                        accentColor = MaterialTheme.colorScheme.error,
                        onClick = onNavigateToTeacherDashboard
                    ) {
                        if (unansweredQuestions.isEmpty()) {
                            Text(
                                "All caught up! No pending questions.",
                                fontSize = BodyFontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "${unansweredQuestions.size} question(s) awaiting your answer",
                                fontSize = BodyFontSize,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Show up to 3 latest unanswered
                            unansweredQuestions.take(3).forEach { q ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = ListItemVerticalPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Circle,
                                        contentDescription = null,
                                        modifier = Modifier.size(BulletIconSize),
                                        tint = when (q.priority.lowercase()) {
                                            "high" -> MaterialTheme.colorScheme.error
                                            "medium" -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            q.question,
                                            fontSize = DetailFontSize,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "From: ${q.studentName ?: "Unknown"} • ${q.priority}",
                                            fontSize = DetailFontSize,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (unansweredQuestions.size > 3) {
                                Text(
                                    "+${unansweredQuestions.size - 3} more…",
                                    fontSize = DetailFontSize,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = ListItemVerticalPadding)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(CardSpacing))

                    // My Classes summary
                    ClassListCard(
                        classes = userClasses,
                        user = user,
                        onClick = onNavigateToClassManagement
                    )

                    Spacer(modifier = Modifier.height(CardSpacing))
                }

                // ===================== STUDENT OVERVIEW =====================
                if (isStudent) {
                    // Question Status Card
                    OverviewCard(
                        title = "My Questions",
                        icon = Icons.Default.History,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToStudentDashboard
                    ) {
                        if (studentQuestions.isEmpty()) {
                            Text(
                                "You haven't asked any questions yet.",
                                fontSize = BodyFontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val answered = studentQuestions.count {
                                it.status.equals("answered", ignoreCase = true) || !it.answer.isNullOrBlank()
                            }
                            val pending = studentQuestions.size - answered

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatChip(
                                    label = "Total",
                                    value = studentQuestions.size.toString(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatChip(
                                    label = "Answered",
                                    value = answered.toString(),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                StatChip(
                                    label = "Pending",
                                    value = pending.toString(),
                                    color = if (pending > 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.outline
                                )
                            }

                            // Show latest unanswered
                            val latestPending = studentQuestions
                                .filter { it.status.equals("open", ignoreCase = true) || it.answer.isNullOrBlank() }
                                .take(2)
                            if (latestPending.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Latest pending:",
                                    fontSize = DetailFontSize,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                latestPending.forEach { q ->
                                    Text(
                                        "• ${q.question}",
                                        fontSize = DetailFontSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = ListItemVerticalPadding)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(CardSpacing))

                    // My Classes & Groups summary for Student
                    ClassListCard(
                        classes = userClasses,
                        user = user,
                        onClick = onNavigateToClassManagement
                    )

                    Spacer(modifier = Modifier.height(CardSpacing))

                    // Quick Actions Card
                    OverviewCard(
                        title = "Quick Actions",
                        icon = Icons.Default.FlashOn,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.History,
                                label = "Questions",
                                onClick = onNavigateToStudentDashboard
                            )
                            QuickActionButton(
                                icon = Icons.Default.Group,
                                label = "Classes & Groups",
                                onClick = onNavigateToGroupManagement
                            )
                            QuickActionButton(
                                icon = Icons.Default.Forum,
                                label = "Forums",
                                onClick = onNavigateToForum
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(CardSpacing))
                }

                // ===================== PARENT OVERVIEW =====================
                if (isParent) {
                    ClassListCard(
                        classes = userClasses,
                        user = user,
                        onClick = onNavigateToForum
                    )

                    Spacer(modifier = Modifier.height(CardSpacing))
                }

                // ===================== ADMIN OVERVIEW =====================
                if (isAdmin) {
                    // System Stats Card
                    OverviewCard(
                        title = "System Overview",
                        icon = Icons.Default.Dashboard,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = null
                    ) {
                        val totalStudents = allUsers.count { it.role.equals("Student", ignoreCase = true) }
                        val totalTeachers = allUsers.count { it.role.equals("Teacher", ignoreCase = true) }
                        val totalAdmins = allUsers.count { it.role.equals("Admin", ignoreCase = true) }
                        val totalParents = allUsers.count { it.role.equals("Parent", ignoreCase = true) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatChip(label = "Classes", value = allClasses.size.toString(), color = MaterialTheme.colorScheme.primary)
                            StatChip(label = "Students", value = totalStudents.toString(), color = MaterialTheme.colorScheme.tertiary)
                            StatChip(label = "Teachers", value = totalTeachers.toString(), color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatChip(label = "Admins", value = totalAdmins.toString(), color = MaterialTheme.colorScheme.error)
                            StatChip(label = "Parents", value = totalParents.toString(), color = MaterialTheme.colorScheme.outline)
                            StatChip(label = "Total", value = allUsers.size.toString(), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(CardSpacing))

                    // Admin Quick Actions
                    OverviewCard(
                        title = "Management",
                        icon = Icons.Default.Settings,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Class,
                                label = "Classes",
                                onClick = onNavigateToClassManagement
                            )
                            QuickActionButton(
                                icon = Icons.Default.SupervisedUserCircle,
                                label = "Users",
                                onClick = onNavigateToAdminTeacherManagement
                            )
                            QuickActionButton(
                                icon = Icons.Default.Forum,
                                label = "Forums",
                                onClick = onNavigateToForum
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(CardSpacing))
                }

                // ===================== RECENT FORUM ACTIVITY (ALL ROLES) =====================
                OverviewCard(
                    title = "Recent Forum Activity",
                    icon = Icons.Default.Forum,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToForum
                ) {
                    if (recentPosts.isEmpty()) {
                        Text(
                            "No recent forum activity in your classes.",
                            fontSize = BodyFontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recentPosts.forEach { post ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = ListItemVerticalPadding),
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Icon(
                                    Icons.Default.Article,
                                    contentDescription = null,
                                    modifier = Modifier.size(ForumIconSize),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        post.title,
                                        fontSize = DetailFontSize,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "by ${post.fullName ?: post.username} • ${post.createdAt}",
                                        fontSize = DetailFontSize,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SectionBottomPadding))
            }
        }
    }
}

// ============== REUSABLE COMPOSABLES ==============

/**
 * Shared "My Classes" card used by Teacher, Parent, and any role that shows a class list.
 */
@Composable
private fun ClassListCard(
    classes: List<ClassEntity>,
    user: User,
    onClick: () -> Unit
) {
    val isAdmin = user.role.equals("Admin", ignoreCase = true)
    val isParent = user.role.equals("Parent", ignoreCase = true)
    
    val cardTitle = if (isAdmin || isParent) "My Classes" else "My Classes & Groups"

    OverviewCard(
        title = cardTitle,
        icon = Icons.Default.Class,
        accentColor = MaterialTheme.colorScheme.primary,
        onClick = onClick
    ) {
        if (classes.isEmpty()) {
            Text(
                "You are not assigned to any classes yet.",
                fontSize = BodyFontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Enrolled in ${classes.size} class(es)",
                fontSize = BodyFontSize
            )
            Spacer(modifier = Modifier.height(4.dp))
            classes.take(4).forEach { cls ->
                Text(
                    "• ${cls.className}",
                    fontSize = DetailFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
            if (classes.size > 4) {
                Text(
                    "+${classes.size - 4} more…",
                    fontSize = DetailFontSize,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = ListItemVerticalPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Column(modifier = Modifier.padding(CardInnerPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = CardHeaderBottomPadding)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(CardIconSize)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = HeaderFontSize,
                    fontWeight = FontWeight.Bold
                )
                if (onClick != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(CardChevronSize)
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 64.dp)
    ) {
        Text(
            text = value,
            fontSize = StatValueFontSize,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = DetailFontSize,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 72.dp)
            .padding(horizontal = 4.dp)
    ) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = DetailFontSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
