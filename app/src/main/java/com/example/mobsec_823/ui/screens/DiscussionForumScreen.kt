package com.example.mobsec_823.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.Comment
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.ForumPost
import com.example.mobsec_823.data.User
import com.example.mobsec_823.ui.rememberProfileBitmap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionForumScreen(
    user: User,
    classId: Int,
    className: String,
    onBackClick: () -> Unit
) {
    var posts by remember { mutableStateOf<List<ForumPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Function to refresh posts
    fun refreshPosts() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                posts = DatabaseHelper.getClassPosts(classId)
            } catch (e: Exception) {
                errorMessage = "Failed to load posts: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Load posts for this class
    LaunchedEffect(classId) {
        refreshPosts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("[$className] Discussion Forum") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshPosts() }, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatePostDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Post"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Loading posts...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                }
            }

            if (!isLoading && posts.isEmpty() && errorMessage == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No posts for $className.",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Be the first to contribute, ${user.username}!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!isLoading && posts.isNotEmpty()) {
                Text(
                    text = "${posts.size} ${if (posts.size == 1) "post" else "posts"} in this class:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Add padding for FAB
                ) {
                    items(posts, key = { it.postId }) { post ->
                        ForumPostCard(
                            post = post,
                            currentUser = user,
                            onPostUpdated = { refreshPosts() },
                            onPostDeleted = { refreshPosts() }
                        )
                    }
                }
            }
        }
    }

    if (showCreatePostDialog) {
        CreatePostDialog(
            onDismiss = { showCreatePostDialog = false },
            onCreatePost = { title, content, imageUrl ->
                scope.launch {
                    val newPost = DatabaseHelper.createPost(classId, user.userId, title, content, imageUrl)
                    if (newPost != null) {
                        refreshPosts()
                    }
                    showCreatePostDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onCreatePost: (title: String, content: String, imageUrl: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create New Post") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, enabled = !isCreating
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("Content *") },
                    modifier = Modifier.fillMaxWidth().height(150.dp), enabled = !isCreating
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = imageUrl, onValueChange = { imageUrl = it },
                    label = { Text("Image URL (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !isCreating
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { isCreating = true; onCreatePost(title, content, imageUrl.ifBlank { null }) },
                enabled = title.isNotBlank() && content.isNotBlank() && !isCreating
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") }
        }
    )
}

@Composable
fun ForumPostCard(
    post: ForumPost,
    currentUser: User,
    onPostUpdated: () -> Unit,
    onPostDeleted: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoadingComments by remember { mutableStateOf(true) }
    var commentsError by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Only the post owner can edit
    val canEdit = post.userId == currentUser.userId
    // Owner, Admin, or Teacher can delete
    val canDelete = post.userId == currentUser.userId ||
            currentUser.role.equals("Admin", ignoreCase = true) ||
            currentUser.role.equals("Teacher", ignoreCase = true)

    fun refreshComments() {
        scope.launch {
            isLoadingComments = true
            commentsError = null
            try {
                comments = DatabaseHelper.getPostComments(post.postId)
            } catch (e: Exception) {
                commentsError = "Failed to load comments"
                e.printStackTrace()
            } finally {
                isLoadingComments = false
            }
        }
    }

    LaunchedEffect(post.postId) { refreshComments() }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val profileBitmap = rememberProfileBitmap(post.profileImage)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = post.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        Text(text = "by ${post.fullName ?: post.username} • ${post.createdAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (canEdit || canDelete) {
                    Row {
                        if (canEdit) {
                            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (canDelete) {
                            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = post.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoadingComments) { isExpanded = !isExpanded }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isLoadingComments && !isExpanded -> "View Comments (...)"
                        isExpanded -> "Hide Comments"
                        else -> "View Comments (${comments.size})"
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand", tint = MaterialTheme.colorScheme.primary
                )
            }

            if (isExpanded) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingComments) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                commentsError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp)) }

                if (!isLoadingComments && comments.isEmpty() && commentsError == null) {
                    Text(text = "No comments yet. Be the first to comment!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                }

                if (!isLoadingComments && comments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                        comments.forEach { comment ->
                            CommentItem(comment = comment, currentUser = currentUser, onCommentUpdated = { refreshComments() }, onCommentDeleted = { refreshComments() })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                AddCommentSection(postId = post.postId, userId = currentUser.userId, onCommentAdded = { refreshComments() })
            }
        }
    }

    if (showEditDialog) {
        EditPostDialog(post = post, onDismiss = { showEditDialog = false }, onUpdatePost = { title, content, imageUrl ->
            scope.launch {
                val success = DatabaseHelper.updatePost(post.postId, currentUser.userId, title, content, imageUrl)
                if (success) onPostUpdated()
                showEditDialog = false
            }
        })
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Delete Post",
            message = "Are you sure you want to delete this post? All comments will also be deleted.",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    val success = DatabaseHelper.deletePost(post.postId, currentUser.userId)
                    showDeleteDialog = false
                    if (success) {
                        onPostDeleted()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostDialog(post: ForumPost, onDismiss: () -> Unit, onUpdatePost: (title: String, content: String, imageUrl: String?) -> Unit) {
    var title by remember { mutableStateOf(post.title) }
    var content by remember { mutableStateOf(post.content) }
    var imageUrl by remember { mutableStateOf(post.imageUrl ?: "") }
    var isUpdating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Edit Post") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !isUpdating)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content *") }, modifier = Modifier.fillMaxWidth().height(150.dp), enabled = !isUpdating)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !isUpdating)
            }
        },
        confirmButton = {
            Button(onClick = { isUpdating = true; onUpdatePost(title, content, imageUrl.ifBlank { null }) }, enabled = title.isNotBlank() && content.isNotBlank() && !isUpdating) {
                if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isUpdating) { Text("Cancel") } }
    )
}

@Composable
fun DeleteConfirmationDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var isDeleting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = { isDeleting = true; onConfirm() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = !isDeleting
            ) {
                if (isDeleting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onError) else Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Cancel") } }
    )
}

@Composable
fun AddCommentSection(postId: Int, userId: Int, onCommentAdded: () -> Unit) {
    var commentText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = commentText, onValueChange = { commentText = it },
            placeholder = { Text("Write a comment...") },
            modifier = Modifier.weight(1f), singleLine = true, enabled = !isPosting
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (commentText.isNotBlank()) {
                    isPosting = true
                    scope.launch {
                        val newComment = DatabaseHelper.createComment(postId, userId, commentText)
                        if (newComment != null) { commentText = ""; onCommentAdded() }
                        isPosting = false
                    }
                }
            },
            enabled = commentText.isNotBlank() && !isPosting
        ) {
            if (isPosting) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Icon(imageVector = Icons.Default.Send, contentDescription = "Post Comment", tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CommentItem(comment: Comment, currentUser: User, onCommentUpdated: () -> Unit, onCommentDeleted: () -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Only the comment owner can edit
    val canEdit = comment.userId == currentUser.userId
    // Owner, Admin, or Teacher can delete
    val canDelete = comment.userId == currentUser.userId ||
            currentUser.role.equals("Admin", ignoreCase = true) ||
            currentUser.role.equals("Teacher", ignoreCase = true)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val profileBitmap = rememberProfileBitmap(comment.profileImage)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${comment.fullName ?: comment.username} • ${comment.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canEdit || canDelete) {
                    Row {
                        if (canEdit) {
                            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (canDelete) {
                            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showEditDialog) {
        EditCommentDialog(comment = comment, onDismiss = { showEditDialog = false }, onUpdateComment = { content ->
            scope.launch {
                val success = DatabaseHelper.updateComment(comment.commentId, currentUser.userId, content)
                if (success) onCommentUpdated()
                showEditDialog = false
            }
        })
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Delete Comment", message = "Are you sure you want to delete this comment?",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    val success = DatabaseHelper.deleteComment(comment.commentId, currentUser.userId)
                    if (success) onCommentDeleted()
                    showDeleteDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCommentDialog(comment: Comment, onDismiss: () -> Unit, onUpdateComment: (content: String) -> Unit) {
    var content by remember { mutableStateOf(comment.content) }
    var isUpdating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Edit Comment") },
        text = {
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Comment") }, modifier = Modifier.fillMaxWidth().height(120.dp), enabled = !isUpdating)
        },
        confirmButton = {
            Button(onClick = { isUpdating = true; onUpdateComment(content) }, enabled = content.isNotBlank() && !isUpdating) {
                if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isUpdating) { Text("Cancel") } }
    )
}
