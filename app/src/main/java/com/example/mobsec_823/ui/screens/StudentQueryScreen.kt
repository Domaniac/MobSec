package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQueryScreen(
    user: User,
    onBackClick: () -> Unit,
    onSubmitQuery: suspend (question: String, priority: String) -> Boolean
) {
    var question by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedPriority by remember { mutableStateOf("Normal") }
    var statusMessage by remember { mutableStateOf("") }

    val priorities = listOf("Low", "Normal", "High")
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Send Question to Teacher", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Your Question") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 🔽 Priority Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedPriority,
                onValueChange = {},
                readOnly = true,
                label = { Text("Priority") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                priorities.forEach { priority ->
                    DropdownMenuItem(
                        text = { Text(priority) },
                        onClick = {
                            selectedPriority = priority
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    if (question.isNotBlank()) {
                        val success = onSubmitQuery(question, selectedPriority)
                        statusMessage = if (success) {
                            "Question sent successfully"
                        } else {
                            "Failed to send question"
                        }
                        question = ""
                        selectedPriority = "Normal"
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusMessage)
        }
    }
}
