package com.example.mobsec_823.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.SecurityUtils
import com.example.mobsec_823.data.api.RegisterResult
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    allowedRoles: List<String>? = null,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Base64 encoded image string
    var profileImageBase64 by remember { mutableStateOf<String?>(null) }
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Use allowedRoles if provided, otherwise show only Student and Parent
    val allRoles = listOf("Student", "Parent")
    val segments = allowedRoles ?: allRoles
    var selectedSegment by remember { mutableIntStateOf(0) }

    // Separate error states for independent UI handling
    var generalErrorMessage by remember { mutableStateOf<String?>(null) }
    var isUsernameDuplicate by remember { mutableStateOf(false) }
    var isIdDuplicate by remember { mutableStateOf(false) }

    // Clear all fields and remove focus when the role tab changes
    LaunchedEffect(selectedSegment) {
        focusManager.clearFocus()
        fullName = ""
        username = ""
        idNumber = ""
        password = ""
        confirmPassword = ""
        profileImageBase64 = null
        profileBitmap = null
        generalErrorMessage = null
        isUsernameDuplicate = false
        isIdDuplicate = false
    }

    var showImageSourceDialog by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Helper to process bitmap to Base64
    fun processBitmap(bitmap: Bitmap) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        profileImageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        profileBitmap = bitmap
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            processBitmap(bitmap)
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                processBitmap(bitmap)
            }
        }
    }

    // Dialog for Image Source
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Profile Picture") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Take Photo") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            cameraLauncher.launch()
                            showImageSourceDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            galleryLauncher.launch("image/*")
                            showImageSourceDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isAdminMode = onBackClick != null

    Scaffold(
        topBar = {
            if (isAdminMode) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { onBackClick?.invoke() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = if (isAdminMode) Arrangement.Top else Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create an Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Profile Picture with Pencil Icon Overlay
        Box(
            modifier = Modifier
                .size(100.dp)
                .clickable { showImageSourceDialog = true },
            contentAlignment = Alignment.BottomEnd
        ) {
            // Profile Picture Circle (Clipped)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap!!.asImageBitmap(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pencil Icon Overlay
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile Picture",
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (profileImageBase64 != null) {
            Text(
                text = "Image selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Role Switcher
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            segments.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = segments.size),
                    onClick = { selectedSegment = index },
                    selected = index == selectedSegment,
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Global Error Message (Only shows if it's not a field-specific duplicate error)
        generalErrorMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Form Fields
        val isFullNameValid = fullName.all { !it.isDigit() }
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name *") },
            modifier = Modifier.fillMaxWidth(),
            isError = fullName.isNotEmpty() && !isFullNameValid
        )

        if (fullName.isNotEmpty() && !isFullNameValid) {
            Text(
                text = "Full name cannot contain numbers",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Username Field with Field-Specific Error
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                isUsernameDuplicate = false // Clear only username error on typing
            },
            label = { Text("Username *") },
            modifier = Modifier.fillMaxWidth(),
            isError = isUsernameDuplicate
        )

        if (isUsernameDuplicate) {
            Text(
                text = "Username already exists",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic Identifier Label
        val currentRole = segments[selectedSegment]
        val idLabel = when (currentRole) {
            "Student" -> "Student Number *"
            "Parent" -> "Child's Student Number *"
            "Teacher" -> "Staff Number *"
            "Admin" -> "Admin Number *"
            else -> "ID Number *"
        }
        val idErrorPrefix = if (currentRole == "Teacher" || currentRole == "Admin") "Employee/Admin Number" else "Student/Parent Number"
        val isIdNumberNumeric = idNumber.all { it.isDigit() }

        // ID Number Field with Field-Specific Error
        OutlinedTextField(
            value = idNumber,
            onValueChange = {
                idNumber = it
                isIdDuplicate = false // Clear only ID error on typing
            },
            label = { Text(idLabel) },
            modifier = Modifier.fillMaxWidth(),
            isError = (idNumber.isNotEmpty() && !isIdNumberNumeric) || isIdDuplicate,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        if (idNumber.isNotEmpty() && !isIdNumberNumeric) {
            Text(
                text = "$idErrorPrefix cannot contain alphabets",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp)
            )
        } else if (isIdDuplicate) {
            Text(
                text = "An account has already exist under this number",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password *") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password *") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = confirmPassword.isNotEmpty() && password != confirmPassword
        )

        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                generalErrorMessage = null
                isUsernameDuplicate = false
                isIdDuplicate = false

                scope.launch {
                    try {
                        val hashedPassword = SecurityUtils.hashPassword(password)
                        val role = segments[selectedSegment]

                        val result = DatabaseHelper.registerUser(
                            username = username,
                            studentEmployeeNumber = idNumber,
                            passwordHash = hashedPassword,
                            role = role,
                            fullName = fullName,
                            profileImageUrl = profileImageBase64
                        )

                        when (result) {
                            is RegisterResult.Success -> onRegisterSuccess()
                            is RegisterResult.Failure -> {
                                val msg = result.message ?: ""
                                val hasUsernameErr = msg.contains("Username already exists", ignoreCase = true)
                                val hasIdErr = msg.contains("An account has already exist under this number", ignoreCase = true)

                                // Explicitly update independent flags
                                if (hasUsernameErr) isUsernameDuplicate = true
                                if (hasIdErr) isIdDuplicate = true

                                // If the message doesn't match known duplicate errors, fallback to general message
                                if (!hasUsernameErr && !hasIdErr) {
                                    generalErrorMessage = msg
                                }
                            }
                        }
                    } catch (e: Exception) {
                        generalErrorMessage = "Registration error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading &&
                    fullName.isNotBlank() &&
                    isFullNameValid &&
                    username.isNotBlank() &&
                    idNumber.isNotBlank() &&
                    isIdNumberNumeric &&
                    password.isNotBlank() &&
                    confirmPassword.isNotBlank() &&
                    password == confirmPassword
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register")
            }
        }

        if (!isAdminMode) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { onNavigateToLogin() },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.heightIn(min = 1.dp)
                ) {
                    Text(
                        text = "Login here.",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
    }
}