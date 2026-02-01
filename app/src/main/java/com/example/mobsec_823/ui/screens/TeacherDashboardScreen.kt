package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.Question
import com.example.mobsec_823.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    teacher: User,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val refreshQuestions = {
        scope.launch {
            isLoading = true
            // Fetch questions assigned to this teacher
            val fetched = DatabaseHelper.getQuestionsForTeacher(teacher.userId)

            // Filter for unanswered and sort by priority: High (3) > Normal (2) > Low (1)
            questions = fetched.filter { it.answer.isNullOrBlank() }
                .sortedByDescending {
                    when (it.priority.lowercase()) {
                        "high" -> 3
                        "normal" -> 2
                        "low" -> 1
                        else -> 0
                    }
                }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshQuestions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Questions") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (questions.isEmpty()) {
                Text(
                    "No unanswered questions found.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(questions) { question ->
                        QuestionCard(
                            question = question,
                            onAnswerSubmitted = { refreshQuestions() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionCard(question: Question, onAnswerSubmitted: () -> Unit) {
    var answerText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Background color based on priority to help teacher prioritize visually
    val priorityColor = when (question.priority.lowercase()) {
        "high" -> Color(0xFFFFEBEE) // Light Red
        "normal" -> Color(0xFFFFFDE7) // Light Yellow
        else -> Color(0xFFF1F8E9) // Light Green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = priorityColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Who asked and When
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "From: ${question.studentName ?: "Unknown Student"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = question.date ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Priority Label
            Text(
                text = "PRIORITY: ${question.priority.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = if (question.priority.lowercase() == "high") Color.Red else Color.DarkGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

            // The Question
            Text(
                text = "Question:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Answer Input Field
            OutlinedTextField(
                value = answerText,
                onValueChange = { answerText = it },
                label = { Text("Type your answer...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                maxLines = 3
            )

            Button(
                onClick = {
                    if (answerText.isNotBlank()) {
                        scope.launch {
                            isSubmitting = true
                            // Calls the PUT /api/questions/<id>/answer endpoint
                            val success = DatabaseHelper.answerQuestion(question.questionId, answerText)
                            if (success) onAnswerSubmitted()
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
                enabled = answerText.isNotBlank() && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Submit Answer")
                }
            }
        }
    }
}