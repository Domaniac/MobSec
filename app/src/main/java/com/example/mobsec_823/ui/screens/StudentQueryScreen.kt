package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.User
import com.example.mobsec_823.data.DatabaseHelper // Assuming this is where your logic lives
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQueryScreen(
    user: User,
    onBackClick: () -> Unit,
    // Updated signature to include teacherId
    onSubmitQuery: suspend (teacherId: Int, question: String, priority: String) -> Boolean
) {
    var question by remember { mutableStateOf("") }
    var priorityExpanded by remember { mutableStateOf(false) }
    var teacherExpanded by remember { mutableStateOf(false) }

    var selectedPriority by remember { mutableStateOf("Normal") }
    var statusMessage by remember { mutableStateOf("") }

    // State for teacher selection
    val teachers = remember { mutableStateListOf<User>() }
    var selectedTeacher by remember { mutableStateOf<User?>(null) }

    val priorities = listOf("Low", "Normal", "High")
    val scope = rememberCoroutineScope()

    // Fetch teachers when the screen loads
    LaunchedEffect(Unit) {
        val fetchedTeachers = DatabaseHelper.getTeachers() // Ensure this method exists in DatabaseHelper
        teachers.clear()
        teachers.addAll(fetchedTeachers)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Send Question to Teacher", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // 🔽 Teacher Selection Dropdown
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

        // 🔽 Priority Dropdown
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
                            selectedTeacher!!.userId, // Use userId from the selected teacher
                            question,
                            selectedPriority
                        )
                        if (success) {
                            statusMessage = "Question sent successfully!"
                            question = ""
                        } else {
                            statusMessage = "Failed to send question."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(statusMessage, color = if (statusMessage.contains("success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
}