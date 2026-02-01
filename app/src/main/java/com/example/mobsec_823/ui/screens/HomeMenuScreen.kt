package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuScreen(
    user: User,
    onNavigateToForum: () -> Unit,
    onNavigateToClassManagement: () -> Unit,
    onNavigateToStudentQuery: () -> Unit,
    onNavigateToStudentDashboard: () -> Unit,
    onNavigateToTeacherDashboard: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MobSec Home") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome, ${user.fullName ?: user.username}!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Role: ${user.role}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "User Information (DEBUG, REMOVE LATER)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    UserInfoRow(label = "Username", value = user.username)
                    UserInfoRow(label = "Full Name", value = user.fullName ?: "N/A")
                    UserInfoRow(label = "Role", value = user.role)
                    UserInfoRow(label = "User ID", value = user.userId.toString())
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select an option:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Discussion Forum button (available to all users)
            Button(
                onClick = onNavigateToForum,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Discussion Forums",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (user.role.equals("Student", ignoreCase = true)) {
                Button(
                    onClick = onNavigateToStudentDashboard,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("My Question History")
                }
            }
            if (user.role == "Teacher") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToTeacherDashboard,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Teacher Dashboard (Questions)")
                }
            }

            // Show Student Query button ONLY if user is a student
            if (user.role == "Student") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateToStudentQuery, modifier = Modifier.fillMaxWidth()) {
                    Text("Ask a Question")
                }
            }

            // Class Management button (only for Teachers and Admins)
            if (user.role.equals("Teacher", ignoreCase = true) ||
                user.role.equals("Admin", ignoreCase = true)) {
                Button(
                    onClick = onNavigateToClassManagement,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Manage Classes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun UserInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

