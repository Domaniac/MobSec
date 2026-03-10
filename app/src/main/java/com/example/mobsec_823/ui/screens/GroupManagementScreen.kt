package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.GroupEntity
import com.example.mobsec_823.data.User
import com.example.mobsec_823.ui.rememberProfileBitmap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    classId: Int,
    user: User,
    onBackClick: () -> Unit,
    showBackButton: Boolean = false
) {
    val isAdminOrTeacher = user.role.equals("Admin", ignoreCase = true) ||
            user.role.equals("Teacher", ignoreCase = true)

    if (isAdminOrTeacher) {
        AdminTeacherGroupManagement(
            classId = classId,
            onBackClick = onBackClick,
            showBackButton = showBackButton
        )
    } else {
        StudentGroupView(
            classId = classId,
            user = user,
            onBackClick = onBackClick,
            showBackButton = showBackButton
        )
    }
}

// ==================== STUDENT VIEW (restricted) ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentGroupView(
    classId: Int,
    user: User,
    onBackClick: () -> Unit,
    showBackButton: Boolean
) {
    var myGroup by remember { mutableStateOf<GroupEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(classId) {
        isLoading = true
        myGroup = DatabaseHelper.getUserGroup(classId, user.userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Classes & Groups") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = if (showBackButton) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                            contentDescription = if (showBackButton) "Back" else "Menu"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text("Group Membership", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val currentGroup = myGroup
                        if (currentGroup != null) {
                            Text("Group: ${currentGroup.group_name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Text("Your Group Members:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            currentGroup.members?.forEach { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val profileBitmap = rememberProfileBitmap(member.profileImage)
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (profileBitmap != null) {
                                                Image(
                                                    bitmap = profileBitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = member.fullName ?: member.username, fontWeight = FontWeight.Medium)
                                    }
                                    if (member.userId == user.userId) {
                                        Text("(You)", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            Text("You are not assigned to any group in this class yet.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "As a student, you can only view members assigned to the same group as you within this class.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==================== ADMIN/TEACHER VIEW (full management) ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTeacherGroupManagement(
    classId: Int,
    onBackClick: () -> Unit,
    showBackButton: Boolean
) {
    var groups by remember { mutableStateOf<List<GroupEntity>>(emptyList()) }
    var allClassUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var unassignedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Create group dialog
    var showCreateDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    // Delete group confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }

    // Manage members dialog
    var showManageMembersDialog by remember { mutableStateOf(false) }
    var targetGroup by remember { mutableStateOf<GroupEntity?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf("Groups", "Unassigned Students")

    val refresh: () -> Unit = {
        scope.launch {
            isLoading = true
            groups = DatabaseHelper.getClassGroups(classId)
            allClassUsers = DatabaseHelper.getClassUsers(classId)
            unassignedUsers = DatabaseHelper.getUnassignedStudents(classId)
            isLoading = false
        }
    }

    LaunchedEffect(classId) {
        refresh()
    }

    // Sync targetGroup when groups list updates
    LaunchedEffect(groups) {
        if (targetGroup != null) {
            targetGroup = groups.find { it.group_id == targetGroup!!.group_id }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Groups") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = if (showBackButton) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                            contentDescription = if (showBackButton) "Back" else "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> GroupsTab(
                        groups = groups,
                        onManageMembers = { group ->
                            targetGroup = group
                            showManageMembersDialog = true
                        },
                        onDeleteGroup = { group ->
                            groupToDelete = group
                            showDeleteDialog = true
                        }
                    )
                    1 -> UnassignedUsersTab(unassignedUsers = unassignedUsers)
                }
            }
        }
    }

    // ===== Create Group Dialog =====
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newGroupName = ""
            },
            title = { Text("Create New Group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            scope.launch {
                                val result = DatabaseHelper.createGroup(classId, newGroupName.trim())
                                if (result != null) {
                                    snackbarHostState.showSnackbar("Group '${result.group_name}' created")
                                    refresh()
                                } else {
                                    snackbarHostState.showSnackbar("Failed to create group")
                                }
                                showCreateDialog = false
                                newGroupName = ""
                            }
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newGroupName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ===== Delete Group Confirmation Dialog =====
    if (showDeleteDialog && groupToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                groupToDelete = null
            },
            title = { Text("Delete Group") },
            text = {
                Text("Are you sure you want to delete \"${groupToDelete!!.group_name}\"? All members will become unassigned.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = DatabaseHelper.deleteGroup(classId, groupToDelete!!.group_id)
                            if (success) {
                                snackbarHostState.showSnackbar("Group deleted")
                                refresh()
                            } else {
                                snackbarHostState.showSnackbar("Failed to delete group")
                            }
                            showDeleteDialog = false
                            groupToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    groupToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ===== Manage Members Dialog (with Toggle Switches) =====
    if (showManageMembersDialog && targetGroup != null) {
        ManageMembersDialog(
            classId = classId,
            group = targetGroup!!,
            allClassUsers = allClassUsers,
            onDismiss = {
                showManageMembersDialog = false
                targetGroup = null
            },
            onRefresh = refresh
        )
    }
}

// ==================== Groups Tab ====================

@Composable
private fun GroupsTab(
    groups: List<GroupEntity>,
    onManageMembers: (GroupEntity) -> Unit,
    onDeleteGroup: (GroupEntity) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No groups created yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap + to create a new group.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(groups, key = { it.group_id }) { group ->
                GroupCard(
                    group = group,
                    onManageMembers = { onManageMembers(group) },
                    onDeleteGroup = { onDeleteGroup(group) }
                )
            }
        }
    }
}

// ==================== Group Card (Collapsible) ====================

@Composable
private fun GroupCard(
    group: GroupEntity,
    onManageMembers: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val memberCount = group.members?.size ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Group header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = group.group_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$memberCount student${if (memberCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onManageMembers) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Manage Members",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteGroup) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Group",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Expand / Collapse toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Members list
            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (group.members.isNullOrEmpty()) {
                    Text(
                        "No students assigned yet. Tap + to manage members.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    group.members.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                val profileBitmap = rememberProfileBitmap(member.profileImage)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileBitmap != null) {
                                        Image(
                                            bitmap = profileBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = member.fullName ?: member.username,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "@${member.username} • ${member.role}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Unassigned Users Tab ====================

@Composable
private fun UnassignedUsersTab(unassignedUsers: List<User>) {
    // Only show students in this tab
    val unassignedStudents = unassignedUsers.filter { it.role.equals("Student", ignoreCase = true) }

    if (unassignedStudents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "All students are assigned to a group!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Text(
                    "${unassignedStudents.size} student${if (unassignedStudents.size != 1) "s" else ""} not assigned to any group",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(unassignedStudents, key = { it.userId }) { userItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val profileBitmap = rememberProfileBitmap(userItem.profileImage)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileBitmap != null) {
                                Image(
                                    bitmap = profileBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userItem.fullName ?: userItem.username,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "@${userItem.username} • ${userItem.role}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Manage Members Dialog (with Toggle Switches) ====================

@Composable
private fun ManageMembersDialog(
    classId: Int,
    group: GroupEntity,
    allClassUsers: List<User>,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var updatingUserId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    // Filter to only show Students in the management dialog
    val filteredStudents = allClassUsers.filter { u ->
        u.role.equals("Student", ignoreCase = true) && (
            (u.fullName ?: u.username).contains(searchQuery, ignoreCase = true) ||
            u.username.contains(searchQuery, ignoreCase = true)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Students: ${group.group_name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search students...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredStudents, key = { it.userId }) { userItem ->
                        val isInThisGroup = group.members?.any { it.userId == userItem.userId } ?: false
                        val isUpdating = updatingUserId == userItem.userId

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val profileBitmap = rememberProfileBitmap(userItem.profileImage)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileBitmap != null) {
                                        Image(
                                            bitmap = profileBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userItem.fullName ?: userItem.username,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "@${userItem.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                if (isUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Switch(
                                        checked = isInThisGroup,
                                        onCheckedChange = { shouldAdd ->
                                            scope.launch {
                                                updatingUserId = userItem.userId
                                                val success = if (shouldAdd) {
                                                    DatabaseHelper.addUserToGroup(classId, group.group_id, userItem.userId)
                                                } else {
                                                    DatabaseHelper.removeUserFromGroup(classId, group.group_id, userItem.userId)
                                                }
                                                if (success) {
                                                    onRefresh()
                                                }
                                                updatingUserId = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
