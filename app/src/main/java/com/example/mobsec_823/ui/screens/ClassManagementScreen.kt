package com.example.mobsec_823.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobsec_823.data.ClassEntity
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassManagementScreen(
    user: User,
    onBackClick: () -> Unit,
    onManageGroups: (ClassEntity) -> Unit = {}
) {
    var classes by remember { mutableStateOf<List<ClassEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(if (user.role.equals("Admin", ignoreCase = true)) 0 else 1) }

    val isAdmin = user.role.equals("Admin", ignoreCase = true)
    val scope = rememberCoroutineScope()

    // Load classes based on role
    val refreshClasses = {
        scope.launch {
            isLoading = true
            classes = if (isAdmin) {
                DatabaseHelper.getAllClasses()
            } else {
                DatabaseHelper.getUserClasses(user.userId)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshClasses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAdmin) "Class Management" else "My Classes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isAdmin) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Create Class") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Manage Classes") }
                    )
                }
            }

            when (selectedTab) {
                0 -> if (isAdmin) {
                    CreateClassTab(onClassCreated = { refreshClasses() })
                }
                1 -> ManageClassesTab(
                    user = user,
                    classes = classes,
                    isLoading = isLoading,
                    onRefresh = { refreshClasses() },
                    onManageGroups = onManageGroups
                )
            }
        }
    }
}

@Composable
fun CreateClassTab(onClassCreated: () -> Unit) {
    var className by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Create New Class", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        OutlinedTextField(
            value = className,
            onValueChange = { className = it; errorMessage = null; successMessage = null },
            label = { Text("Class Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (className.isBlank()) { errorMessage = "Class name cannot be empty"; return@Button }
                scope.launch {
                    isCreating = true
                    val createdClass = DatabaseHelper.createClass(className)
                    if (createdClass != null) {
                        successMessage = "Class '${createdClass.className}' created!"; className = ""; onClassCreated()
                    } else { errorMessage = "Failed to create class" }
                    isCreating = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating
        ) {
            if (isCreating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Create Class")
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
        successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp)) }
    }
}

@Composable
fun ManageClassesTab(
    user: User,
    classes: List<ClassEntity>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onManageGroups: (ClassEntity) -> Unit
) {
    var selectedClass by remember { mutableStateOf<ClassEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManageUsersDialog by remember { mutableStateOf(false) }
    val isAdmin = user.role.equals("Admin", ignoreCase = true)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (isAdmin) "All Classes" else "Assigned Classes", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (classes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (isAdmin) "No classes found." else "No classes assigned yet.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(classes) { classEntity ->
                    ClassCard(
                        user = user,
                        classEntity = classEntity,
                        onEdit = { selectedClass = classEntity; showEditDialog = true },
                        onDelete = { selectedClass = classEntity; showDeleteDialog = true },
                        onManageUsers = { selectedClass = classEntity; showManageUsersDialog = true },
                        onManageGroups = { onManageGroups(classEntity) }
                    )
                }
            }
        }
    }

    if (showEditDialog && selectedClass != null) EditClassDialog(selectedClass!!, onDismiss = { showEditDialog = false }, onConfirm = { onRefresh(); showEditDialog = false })
    if (showDeleteDialog && selectedClass != null) DeleteClassDialog(selectedClass!!, onDismiss = { showDeleteDialog = false }, onConfirm = { onRefresh(); showDeleteDialog = false })
    if (showManageUsersDialog && selectedClass != null) ManageClassUsersDialog(user, selectedClass!!, onDismiss = { showManageUsersDialog = false })
}

@Composable
fun ClassCard(user: User, classEntity: ClassEntity, onEdit: () -> Unit, onDelete: () -> Unit, onManageUsers: () -> Unit, onManageGroups: () -> Unit) {
    val isAdmin = user.role.equals("Admin", ignoreCase = true)
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(classEntity.className, style = MaterialTheme.typography.titleLarge)
            Text("Class ID: ${classEntity.classId}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onManageUsers, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isAdmin) "Users" else "View Users", fontSize = 12.sp)
                }
                Button(onClick = onManageGroups, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Groups", fontSize = 12.sp)
                }
            }
            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
fun EditClassDialog(classEntity: ClassEntity, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var name by remember { mutableStateOf(classEntity.className) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Class") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Class Name") }) },
        confirmButton = { Button(onClick = { scope.launch { if (DatabaseHelper.updateClass(classEntity.classId, name)) onConfirm() } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteClassDialog(classEntity: ClassEntity, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Class") },
        text = { Text("Delete '${classEntity.className}'?") },
        confirmButton = { Button(onClick = { scope.launch { if (DatabaseHelper.deleteClass(classEntity.classId)) onConfirm() } }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ManageClassUsersDialog(user: User, classEntity: ClassEntity, onDismiss: () -> Unit) {
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var classUserIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    var pendingUserId by remember { mutableStateOf<Int?>(null) }
    var searchKeyword by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val tabs = listOf("Student", "Parent", "Teacher")
    
    var viewingUser by remember { mutableStateOf<User?>(null) }

    val isAdmin = user.role.equals("Admin", ignoreCase = true)
    val scope = rememberCoroutineScope()

    suspend fun loadData() {
        isLoading = true
        errorMessage = null
        allUsers = DatabaseHelper.getAllUsers()
        classUserIds = DatabaseHelper.getClassUsers(classEntity.classId).map { it.userId }.toSet()
        isLoading = false
    }

    LaunchedEffect(classEntity.classId) {
        loadData()
    }

    if (viewingUser != null) {
        ViewUserProfileDialog(user = viewingUser!!, onDismiss = { viewingUser = null })
    }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text(if (isAdmin) "Class Users" else "Class Members") },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.8f)) {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = { Text("Search users...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    enabled = !isLoading && !isUpdating
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isAdmin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Users in class: ${classUserIds.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { scope.launch { loadData() } },
                            enabled = !isLoading && !isUpdating
                        ) { Text("Refresh") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val selectedRole = tabs[selectedTabIndex]
                    val filtered = allUsers.filter { u ->
                        val matchesSearch = (u.fullName ?: u.username).contains(searchKeyword, true)
                        val matchesRole = u.role.equals(selectedRole, ignoreCase = true)
                        if (isAdmin) {
                            // Admin sees ALL users (with toggle to add/remove)
                            matchesSearch && matchesRole
                        } else {
                            // Non-admin only sees users already in class
                            val isMember = classUserIds.contains(u.userId)
                            matchesSearch && matchesRole && isMember
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (isAdmin) "No ${selectedRole.lowercase()}s found."
                                else "No ${selectedRole.lowercase()}s found in this class.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(filtered, key = { it.userId }) { item ->
                                val isInClass = classUserIds.contains(item.userId)
                                val isThisUserUpdating = pendingUserId == item.userId

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isInClass)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewingUser = item }
                                            .padding(vertical = 8.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Profile Icon
                                        val profileBitmap = remember(item.profileImageUrl) {
                                            if (item.profileImageUrl != null && item.profileImageUrl.startsWith("data:image")) {
                                                try {
                                                    val base64String = item.profileImageUrl.substringAfter(",")
                                                    val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                                                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            } else {
                                                null
                                            }
                                        }

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

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.fullName ?: item.username, fontWeight = FontWeight.Bold)
                                            Text(item.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // Toggle switch for admin to add/remove users
                                        if (isAdmin) {
                                            if (isThisUserUpdating) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Switch(
                                                    checked = isInClass,
                                                    onCheckedChange = { shouldAdd ->
                                                        scope.launch {
                                                            isUpdating = true
                                                            pendingUserId = item.userId
                                                            errorMessage = null

                                                            val success = if (shouldAdd) {
                                                                DatabaseHelper.addUserToClass(classEntity.classId, item.userId)
                                                            } else {
                                                                DatabaseHelper.removeUserFromClass(classEntity.classId, item.userId)
                                                            }

                                                            if (success) {
                                                                classUserIds = DatabaseHelper.getClassUsers(classEntity.classId).map { it.userId }.toSet()
                                                            } else {
                                                                errorMessage = "Failed to update user. Try again."
                                                            }

                                                            pendingUserId = null
                                                            isUpdating = false
                                                        }
                                                    },
                                                    enabled = !isUpdating
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, enabled = !isUpdating) { Text("Close") } }
    )
}

@Composable
fun ViewUserProfileDialog(user: User, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Profile") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val profileBitmap = remember(user.profileImageUrl) {
                    if (user.profileImageUrl != null && user.profileImageUrl.startsWith("data:image")) {
                        try {
                            val base64String = user.profileImageUrl.substringAfter(",")
                            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
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
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = user.fullName ?: user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "@${user.username}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(24.dp))

                ProfileInfoRow(label = "Account Created", value = user.createdAt.substringBefore(" "))
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp)) // Shortened gap for better balance
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
