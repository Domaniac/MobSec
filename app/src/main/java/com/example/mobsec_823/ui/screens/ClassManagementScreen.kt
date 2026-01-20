package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.ClassEntity
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassManagementScreen(
    user: User,
    onBackClick: () -> Unit
) {
    var classes by remember { mutableStateOf<List<ClassEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    // Load classes on start
    LaunchedEffect(Unit) {
        isLoading = true
        classes = DatabaseHelper.getAllClasses()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Management") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Back")
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

            when (selectedTab) {
                0 -> CreateClassTab(
                    onClassCreated = {
                        scope.launch {
                            isLoading = true
                            classes = DatabaseHelper.getAllClasses()
                            isLoading = false
                        }
                    }
                )
                1 -> ManageClassesTab(
                    classes = classes,
                    isLoading = isLoading,
                    onRefresh = {
                        scope.launch {
                            isLoading = true
                            classes = DatabaseHelper.getAllClasses()
                            isLoading = false
                        }
                    }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Create New Class",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = className,
            onValueChange = {
                className = it
                errorMessage = null
                successMessage = null
            },
            label = { Text("Class Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (className.isBlank()) {
                    errorMessage = "Class name cannot be empty"
                    return@Button
                }

                scope.launch {
                    isCreating = true
                    errorMessage = null
                    successMessage = null

                    val createdClass = DatabaseHelper.createClass(className)
                    if (createdClass != null) {
                        successMessage = "Class '${createdClass.className}' created successfully!"
                        className = ""
                        onClassCreated()
                    } else {
                        errorMessage = "Failed to create class. Please try again."
                    }

                    isCreating = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Create Class")
        }

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        successMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ManageClassesTab(
    classes: List<ClassEntity>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    var selectedClass by remember { mutableStateOf<ClassEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManageUsersDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All Classes (${classes.size})",
                style = MaterialTheme.typography.headlineSmall
            )

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Refresh"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (classes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No classes found. Create one in the 'Create Class' tab!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(classes) { classEntity ->
                    ClassCard(
                        classEntity = classEntity,
                        onEdit = {
                            selectedClass = classEntity
                            showEditDialog = true
                        },
                        onDelete = {
                            selectedClass = classEntity
                            showDeleteDialog = true
                        },
                        onManageUsers = {
                            selectedClass = classEntity
                            showManageUsersDialog = true
                        }
                    )
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && selectedClass != null) {
        EditClassDialog(
            classEntity = selectedClass!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                onRefresh()
                showEditDialog = false
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && selectedClass != null) {
        DeleteClassDialog(
            classEntity = selectedClass!!,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onRefresh()
                showDeleteDialog = false
            }
        )
    }

    // Manage Users Dialog
    if (showManageUsersDialog && selectedClass != null) {
        ManageClassUsersDialog(
            classEntity = selectedClass!!,
            onDismiss = { showManageUsersDialog = false }
        )
    }
}

@Composable
fun ClassCard(
    classEntity: ClassEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageUsers: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = classEntity.className,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Class ID: ${classEntity.classId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onManageUsers,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Manage Users",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Users")
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun EditClassDialog(
    classEntity: ClassEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newClassName by remember { mutableStateOf(classEntity.className) }
    var isUpdating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Edit Class") },
        text = {
            Column {
                OutlinedTextField(
                    value = newClassName,
                    onValueChange = {
                        newClassName = it
                        errorMessage = null
                    },
                    label = { Text("Class Name") },
                    enabled = !isUpdating,
                    singleLine = true
                )

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
        confirmButton = {
            TextButton(
                onClick = {
                    if (newClassName.isBlank()) {
                        errorMessage = "Class name cannot be empty"
                        return@TextButton
                    }

                    scope.launch {
                        isUpdating = true
                        val success = DatabaseHelper.updateClass(classEntity.classId, newClassName)
                        if (success) {
                            onConfirm(newClassName)
                        } else {
                            errorMessage = "Failed to update class"
                        }
                        isUpdating = false
                    }
                },
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteClassDialog(
    classEntity: ClassEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text("Delete Class") },
        text = {
            Column {
                Text("Are you sure you want to delete '${classEntity.className}'?")
                Text(
                    text = "This action cannot be undone and will remove all user associations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )

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
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        isDeleting = true
                        val success = DatabaseHelper.deleteClass(classEntity.classId)
                        if (success) {
                            onConfirm()
                        } else {
                            errorMessage = "Failed to delete class"
                        }
                        isDeleting = false
                    }
                },
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ManageClassUsersDialog(
    classEntity: ClassEntity,
    onDismiss: () -> Unit
) {
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var classUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingUserId by remember { mutableStateOf<Int?>(null) } // Track which user is being updated

    val scope = rememberCoroutineScope()

    // Function to load all data
    suspend fun loadData() {
        isLoading = true
        loadError = false
        errorMessage = null

        val users = DatabaseHelper.getAllUsers()
        val classUsersList = DatabaseHelper.getClassUsers(classEntity.classId)

        // Check if we got valid data
        if (users.isEmpty() && classUsersList.isEmpty()) {
            // Could be error or genuinely empty - but if allUsers is empty, that's suspicious
            println("WARNING: Both user lists are empty - possible load error")
        }

        allUsers = users
        classUsers = classUsersList
        isLoading = false

        // If we couldn't load users at all, mark as error
        if (users.isEmpty()) {
            loadError = true
            errorMessage = "Failed to load users. Tap Retry."
        }
    }

    // Load users on start
    LaunchedEffect(Unit) {
        loadData()
    }

    val filteredUsers = remember(allUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.fullName?.contains(searchQuery, ignoreCase = true) == true ||
                user.role.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val classUserIds = remember(classUsers) {
        classUsers.map { it.userId }.toSet()
    }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Manage Users - ${classEntity.className}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search users...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading && !isUpdating
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Users in class: ${classUsers.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Refresh button
                    TextButton(
                        onClick = {
                            scope.launch {
                                loadData()
                            }
                        },
                        enabled = !isLoading && !isUpdating
                    ) {
                        Text(if (loadError) "Retry" else "Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading users...")
                        }
                    }
                } else if (loadError && allUsers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Failed to load users",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                scope.launch { loadData() }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredUsers, key = { it.userId }) { user ->
                            val isInClass = classUserIds.contains(user.userId)
                            val isThisUserUpdating = pendingUserId == user.userId

                            UserListItem(
                                user = user,
                                isInClass = isInClass,
                                isUpdating = isThisUserUpdating,
                                enabled = !isUpdating,
                                onToggle = { shouldAdd ->
                                    scope.launch {
                                        isUpdating = true
                                        pendingUserId = user.userId
                                        errorMessage = null

                                        val success = if (shouldAdd) {
                                            DatabaseHelper.addUserToClass(classEntity.classId, user.userId)
                                        } else {
                                            DatabaseHelper.removeUserFromClass(classEntity.classId, user.userId)
                                        }

                                        if (success) {
                                            // Refresh class users with retry
                                            val newClassUsers = DatabaseHelper.getClassUsers(classEntity.classId)
                                            classUsers = newClassUsers
                                            errorMessage = null
                                        } else {
                                            errorMessage = "Failed to update user. Try again."
                                        }

                                        pendingUserId = null
                                        isUpdating = false
                                    }
                                }
                            )
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
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun UserListItem(
    user: User,
    isInClass: Boolean,
    isUpdating: Boolean = false,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
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
                .then(
                    if (enabled && !isUpdating) {
                        Modifier.clickable { onToggle(!isInClass) }
                    } else {
                        Modifier
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName ?: user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "${user.username} • ${user.role}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Switch(
                    checked = isInClass,
                    onCheckedChange = { if (enabled) onToggle(it) },
                    enabled = enabled
                )
            }
        }
    }
}
