package com.example.mobsec_823.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.Question
import com.example.mobsec_823.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    user: User,
    onBackClick: () -> Unit,
    onAddQuestion: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var questionToEdit by remember { mutableStateOf<Question?>(null) }
    var questionToDelete by remember { mutableStateOf<Question?>(null) }

    val refreshQuestions = {
        scope.launch {
            isLoading = true
            questions = DatabaseHelper.getQuestionsByStudent(user.userId)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshQuestions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Question History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshQuestions() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddQuestion,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ask Question")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (questions.isEmpty()) {
                Text("You haven't asked any questions yet.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questions) { question ->
                        StudentQuestionCard(
                            question = question,
                            onEditClick = { questionToEdit = it },
                            onDeleteClick = { questionToDelete = it }
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (questionToEdit != null) {
        EditQuestionDialog(
            question = questionToEdit!!,
            onDismiss = { questionToEdit = null },
            onConfirm = { newText, newPriority ->
                scope.launch {
                    val success = DatabaseHelper.updateQuestion(
                        questionToEdit!!.questionId,
                        user.userId,
                        newText,
                        newPriority
                    )
                    if (success) refreshQuestions()
                    questionToEdit = null
                }
            }
        )
    }

    // Delete Confirmation
    if (questionToDelete != null) {
        AlertDialog(
            onDismissRequest = { questionToDelete = null },
            title = { Text("Delete Question") },
            text = { Text("Are you sure you want to delete this question?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = DatabaseHelper.deleteQuestion(questionToDelete!!.questionId, user.userId)
                            if (success) refreshQuestions()
                            questionToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { questionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentQuestionCard(
    question: Question,
    onEditClick: (Question) -> Unit,
    onDeleteClick: (Question) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val isPending = question.answer.isNullOrBlank()
    val statusColor = if (isPending) Color.Gray else Color(0xFF4CAF50)
    val statusText = if (isPending) "Pending" else "Answered"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = question.date ?: "", style = MaterialTheme.typography.labelSmall)
                }
                
                // Show Edit/Delete only if Pending
                if (isPending) {
                    Row {
                        IconButton(onClick = { onEditClick(question) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteClick(question) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "To: ${question.teacherName ?: "Unknown Teacher"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Q: ${question.question}", style = MaterialTheme.typography.bodyLarge)

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Answer:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = question.answer ?: "The teacher hasn't replied yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (question.answer.isNullOrBlank()) Color.Gray else Color.Unspecified
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Priority: ${question.priority.uppercase()}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (!expanded) {
                Text(
                    text = if (isPending) "Click to view details" else "Click to view answer",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun EditQuestionDialog(
    question: Question,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf(question.question) }
    var priority by remember { mutableStateOf(question.priority) }
    var priorityExpanded by remember { mutableStateOf(false) }
    val priorities = listOf("Low", "Normal", "High")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Question") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        modifier = Modifier.fillMaxWidth().clickable { priorityExpanded = true },
                        trailingIcon = {
                            IconButton(onClick = { priorityExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        priorities.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    priority = p
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text, priority) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
