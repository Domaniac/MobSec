package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.User
import kotlinx.coroutines.launch

@Composable
fun LoginTestScreen(onUserSelected: (User) -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var fetchedUser by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Database Connection Test",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Select a user to simulate login:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Button to fetch User ID 1
        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                fetchedUser = null
                scope.launch {
                    try {
                        val user = DatabaseHelper.getUserById(1)
                        if (user != null) {
                            fetchedUser = user
                            errorMessage = null
                        } else {
                            errorMessage = "User ID 1 not found in database"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            Text("Fetch User ID 1")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to fetch User ID 2
        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                fetchedUser = null
                scope.launch {
                    try {
                        val user = DatabaseHelper.getUserById(2)
                        if (user != null) {
                            fetchedUser = user
                            errorMessage = null
                        } else {
                            errorMessage = "User ID 2 not found in database"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            Text("Fetch User ID 2")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator()
            Text(
                text = "Connecting to database...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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

        // Display fetched user data
        fetchedUser?.let { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "User Data Fetched:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text("User ID: ${user.userId}")
                    Text("Username: ${user.username}")
                    Text("Full Name: ${user.fullName ?: "N/A"}")
                    Text("Role: ${user.role}")
                    Text("Created At: ${user.createdAt}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onUserSelected(user) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go to Discussion Forum →")
                    }
                }
            }
        }
    }
}
