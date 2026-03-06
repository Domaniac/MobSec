package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.User

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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MobSec Home") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome, ${user.fullName ?: user.username}!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
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
                    HomeUserInfoRow(label = "Username", value = user.username)
                    HomeUserInfoRow(label = "Full Name", value = user.fullName ?: "N/A")
                    HomeUserInfoRow(label = "Role", value = user.role)
                    HomeUserInfoRow(label = "User ID", value = user.userId.toString())
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select an option:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onNavigateToLocationSharing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary // Different color to stand out
                )
            ) {
                Text(
                    text = "Share & View Locations",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val buttonModifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(bottom = 12.dp)

            // Profile button
            Button(
                onClick = onNavigateToProfile,
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Discussion Forum button (available to all users)
            Button(
                onClick = onNavigateToForum,
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Discussion Forums",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Resource Library button (available to all users)
            Button(
                onClick = onNavigateToResourceLibrary,
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Resource Library",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Student Specific Options
            if (user.role.equals("Student", ignoreCase = true)) {
                Button(
                    onClick = onNavigateToStudentDashboard,
                    modifier = buttonModifier
                ) {
                    Text("My Question History", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = onNavigateToStudentQuery,
                    modifier = buttonModifier
                ) {
                    Text("Ask a Question", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = onNavigateToGroupManagement,
                    modifier = buttonModifier
                ) {
                    Text("My Group & Members", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Teacher Specific Options
            if (user.role.equals("Teacher", ignoreCase = true)) {
                Button(
                    onClick = onNavigateToTeacherDashboard,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Teacher Dashboard (Questions)", style = MaterialTheme.typography.titleMedium)
                }
            }

            val isAdmin = user.role.equals("Admin", ignoreCase = true)
            val isTeacher = user.role.equals("Teacher", ignoreCase = true)

            // Class Management and Group Management buttons (only for Teachers and Admins)
            if (isAdmin || isTeacher) {
                Button(
                    onClick = onNavigateToClassManagement,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = if (isAdmin) "Manage Classes" else "View Classes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onNavigateToGroupManagement,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(
                        text = "Manage Groups",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Admin-only: Manage Admins & Teachers
            if (isAdmin) {
                Button(
                    onClick = onNavigateToAdminTeacherManagement,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Manage Admins & Teachers",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeUserInfoRow(label: String, value: String) {
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
