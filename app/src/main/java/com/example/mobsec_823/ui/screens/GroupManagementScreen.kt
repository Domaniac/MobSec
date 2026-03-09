package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    var unassignedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Create group dialog
    var showCreateDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    // Delete group confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }

    // Add member dialog
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var targetGroup by remember { mutableStateOf<GroupEntity?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf("Groups", "Unassigned Users")

    val refresh: () -> Unit = {
        scope.launch {
            isLoading = true
            groups = DatabaseHelper.getClassGroups(classId)
            unassignedUsers = DatabaseHelper.getUnassignedStudents(classId)
            isLoading = false
        }
    }

    LaunchedEffect(classId) {
        refresh()
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
                        onAddMember = { group ->
                            targetGroup = group
                            showAddMemberDialog = true
                        },
                        onRemoveMember = { group, member ->
                            scope.launch {
                                val success = DatabaseHelper.removeUserFromGroup(classId, group.group_id, member.userId)
                                if (success) {
                                    snackbarHostState.showSnackbar("${member.fullName ?: member.username} removed from ${group.group_name}")
                                    refresh()
                                } else {
                                    snackbarHostState.showSnackbar("Failed to remove member")
                                }
                            }
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

    // ===== Add Member Dialog =====
    if (showAddMemberDialog && targetGroup != null) {
        AddMemberDialog(
            group = targetGroup!!,
            unassignedUsers = unassignedUsers,
            onDismiss = {
                showAddMemberDialog = false
                targetGroup = null
            },
            onAddMember = { selectedUser ->
                scope.launch {
                    val success = DatabaseHelper.addUserToGroup(classId, targetGroup!!.group_id, selectedUser.userId)
                    if (success) {
                        snackbarHostState.showSnackbar("${selectedUser.fullName ?: selectedUser.username} added to ${targetGroup!!.group_name}")
                        refresh()
                    } else {
                        snackbarHostState.showSnackbar("Failed to add member")
                    }
                    showAddMemberDialog = false
                    targetGroup = null
                }
            }
        )
    }
}

// ==================== Groups Tab ====================

@Composable
private fun GroupsTab(
    groups: List<GroupEntity>,
    onAddMember: (GroupEntity) -> Unit,
    onRemoveMember: (GroupEntity, User) -> Unit,
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
                    onAddMember = { onAddMember(group) },
                    onRemoveMember = { member -> onRemoveMember(group, member) },
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
    onAddMember: () -> Unit,
    onRemoveMember: (User) -> Unit,
    onDeleteGroup: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val memberCount = group.members?.size ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Group header row — always visible
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
                        text = "$memberCount member${if (memberCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddMember) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Member",
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

            // Members list — only shown when expanded
            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (group.members.isNullOrEmpty()) {
                    Text(
                        "No members yet. Tap + to add members.",
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
                            IconButton(onClick = { onRemoveMember(member) }) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove ${member.username}",
                                    tint = MaterialTheme.colorScheme.error
                                )
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
    if (unassignedUsers.isEmpty()) {
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
                    "All users are assigned to a group!",
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
                    "${unassignedUsers.size} user${if (unassignedUsers.size != 1) "s" else ""} not assigned to any group",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(unassignedUsers, key = { it.userId }) { userItem ->
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

// ==================== Add Member Dialog ====================

@Composable
private fun AddMemberDialog(
    group: GroupEntity,
    unassignedUsers: List<User>,
    onDismiss: () -> Unit,
    onAddMember: (User) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = unassignedUsers.filter { u ->
        (u.fullName ?: u.username).contains(searchQuery, ignoreCase = true) ||
                u.username.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member to ${group.group_name}") },
        text = {
            Column {
                if (unassignedUsers.isEmpty()) {
                    Text(
                        "No unassigned users available to add.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search users...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredUsers, key = { it.userId }) { userItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAddMember(userItem) },
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
                        if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                Text(
                                    "No users found matching \"$searchQuery\"",
                                    modifier = Modifier.padding(8.dp),
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
