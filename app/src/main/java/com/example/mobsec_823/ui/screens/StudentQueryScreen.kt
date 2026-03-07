package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.User
import com.example.mobsec_823.data.DatabaseHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQueryScreen(
    user: User,
    onBackClick: () -> Unit,
    onSubmitQuery: suspend (teacherId: Int, question: String, priority: String) -> Boolean
) {
    var question by remember { mutableStateOf("") }
    var priorityExpanded by remember { mutableStateOf(false) }
    var teacherExpanded by remember { mutableStateOf(false) }

    var selectedPriority by remember { mutableStateOf("Normal") }
    var statusMessage by remember { mutableStateOf("") }

    val teachers = remember { mutableStateListOf<User>() }
    var selectedTeacher by remember { mutableStateOf<User?>(null) }

    val priorities = listOf("Low", "Normal", "High")
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val fetchedTeachers = DatabaseHelper.getTeachers()
        teachers.clear()
        teachers.addAll(fetchedTeachers)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ask a Question") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Send Question to Teacher", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = teacherExpanded,
                onExpandedChange = { teacherExpanded = !teacherExpanded }
            ) {
                OutlinedTextField(
                    value = selectedTeacher?.fullName ?: "Select a Teacher",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Teacher") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(teacherExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = teacherExpanded,
                    onDismissRequest = { teacherExpanded = false }
                ) {
                    teachers.forEach { teacher ->
                        DropdownMenuItem(
                            text = { teacher.fullName?.let { Text(it) } },
                            onClick = {
                                selectedTeacher = teacher
                                teacherExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = !priorityExpanded }
            ) {
                OutlinedTextField(
                    value = selectedPriority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false }
                ) {
                    priorities.forEach { priority ->
                        DropdownMenuItem(
                            text = { Text(priority) },
                            onClick = {
                                selectedPriority = priority
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Your Question") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (question.isNotBlank() && selectedTeacher != null) {
                            val success = onSubmitQuery(
                                selectedTeacher!!.userId,
                                question,
                                selectedPriority
                            )
                            if (success) {
                                statusMessage = "Question sent successfully!"
                                question = ""
                                // Navigate back after success
                                onBackClick()
                            } else {
                                statusMessage = "Failed to send question."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = question.isNotBlank() && selectedTeacher != null
            ) {
                Text("Send")
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusMessage, 
                    color = if (statusMessage.contains("success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
