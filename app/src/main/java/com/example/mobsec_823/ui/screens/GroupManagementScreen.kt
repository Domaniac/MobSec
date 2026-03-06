package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.GroupEntity
import com.example.mobsec_823.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    classId: Int,
    user: User,
    onBackClick: () -> Unit,
    showBackButton: Boolean = false
) {
    var myGroup by remember { mutableStateOf<GroupEntity?>(null) }
    var unassignedStudents by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(classId) {
        isLoading = true
        myGroup = DatabaseHelper.getUserGroup(classId, user.userId)
        unassignedStudents = DatabaseHelper.getUnassignedStudents(classId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Group & Members") },
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
                // --- SECTION: MY GROUP MEMBERS ---
                Text("My Group Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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

                            currentGroup.members?.forEach { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = member.username, fontWeight = FontWeight.Medium)
                                    if (member.userId == user.userId) {
                                        Text("(You)", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            Text("You are not assigned to a group in this class.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SECTION: FIND MEMBERS (Unassigned Students) ---
                Text("Available Classmates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Students not currently in a group:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    if (unassignedStudents.isEmpty()) {
                        item {
                            Text("No unassigned students found.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                        }
                    } else {
                        items(unassignedStudents) { student ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, Color.LightGray, MaterialTheme.shapes.small)
                                    .padding(12.dp)
                            ) {
                                Text(text = student.username, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
