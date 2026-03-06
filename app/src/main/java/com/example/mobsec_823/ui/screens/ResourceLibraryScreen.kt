package com.example.mobsec_823.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.ResourceEntity
import com.example.mobsec_823.data.User
import com.example.mobsec_823.data.api.SimpleApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceLibraryScreen(
    user: User,
    classId: Int,
    className: String,
    onBackClick: () -> Unit
) {
    var resources by remember { mutableStateOf<List<ResourceEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // PDF Viewing state
    var viewingPdfResource by remember { mutableStateOf<ResourceEntity?>(null) }
    var localPdfFile by remember { mutableStateOf<File?>(null) }
    var isPreparingPdf by remember { mutableStateOf(false) }
    
    // Deletion confirmation state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var resourceToDelete by remember { mutableStateOf<ResourceEntity?>(null) }

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val refreshResources = {
        scope.launch {
            if (resources.isEmpty()) isLoading = true
            val fetched = DatabaseHelper.getClassResources(classId)
            resources = fetched
            isLoading = false
        }
    }

    LaunchedEffect(classId) {
        refreshResources()
    }

    BackHandler(enabled = viewingPdfResource != null) {
        viewingPdfResource = null
        localPdfFile = null
    }

    if (showDeleteDialog && resourceToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                resourceToDelete = null
            },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete \"${resourceToDelete!!.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = resourceToDelete!!
                        scope.launch {
                            val success = DatabaseHelper.deleteResource(toDelete.resourceId)
                            if (success) {
                                resources = resources.filter { it.resourceId != toDelete.resourceId }
                                Toast.makeText(context, "Resource deleted", Toast.LENGTH_SHORT).show()
                                refreshResources()
                            } else {
                                Toast.makeText(context, "Failed to delete resource", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeleteDialog = false
                        resourceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    resourceToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (viewingPdfResource != null) viewingPdfResource!!.title else "Resources: $className",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewingPdfResource != null) {
                            viewingPdfResource = null
                            localPdfFile = null
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewingPdfResource != null && localPdfFile != null) {
                        IconButton(onClick = {
                            scope.launch {
                                val success = saveFileToDownloads(context, localPdfFile!!, viewingPdfResource!!.fileName ?: "document.pdf")
                                if (success) {
                                    Toast.makeText(context, "File saved to Downloads", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    } else if (viewingPdfResource == null && (user.role.equals("teacher", ignoreCase = true) || user.role.equals("admin", ignoreCase = true))) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Resource")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewingPdfResource != null) {
                if (isPreparingPdf) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (localPdfFile != null) {
                    PdfRendererView(file = localPdfFile!!)
                } else {
                    Text("Error loading PDF preview.", modifier = Modifier.align(Alignment.Center))
                }
            } else if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (resources.isEmpty()) {
                Text("No resources shared yet.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(resources, key = { it.resourceId }) { resource ->
                        ResourceCard(
                            resource = resource,
                            showDelete = (user.role.equals("teacher", ignoreCase = true) || user.role.equals("admin", ignoreCase = true)),
                            onDelete = {
                                resourceToDelete = resource
                                showDeleteDialog = true
                            },
                            onClick = {
                                if (resource.resourceType == "pdf") {
                                    scope.launch {
                                        isPreparingPdf = true
                                        viewingPdfResource = resource
                                        val bytes = SimpleApi.getBytes("/api/resources/${resource.resourceId}/view")
                                        if (bytes != null && bytes.isNotEmpty()) {
                                            val tempFile = File(context.cacheDir, "preview_${resource.resourceId}.pdf")
                                            tempFile.writeBytes(bytes)
                                            localPdfFile = tempFile
                                        } else {
                                            Toast.makeText(context, "Server error: Could not fetch PDF data", Toast.LENGTH_LONG).show()
                                            viewingPdfResource = null
                                        }
                                        isPreparingPdf = false
                                    }
                                } else {
                                    resource.url?.let { uriHandler.openUri(it) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddResourceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, url, type, fileUri, fileName ->
                scope.launch {
                    val success = DatabaseHelper.createResource(
                        classId = classId,
                        teacherId = user.userId,
                        title = title,
                        description = desc,
                        resourceType = type,
                        url = url,
                        fileUri = fileUri,
                        context = context,
                        fileName = fileName
                    )
                    if (success) {
                        showAddDialog = false
                        refreshResources()
                    } else {
                        Toast.makeText(context, "Failed to share resource", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String, Uri?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var resourceType by remember { mutableStateOf("link") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                selectedFileUri = it
                selectedFileName = getFileName(context, it)
                resourceType = "pdf"
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share New Resource") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Resource Type:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = resourceType == "link",
                        onClick = { resourceType = "link" }
                    )
                    Text("Link/URL")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = resourceType == "pdf",
                        onClick = { resourceType = "pdf" }
                    )
                    Text("PDF File")
                }

                if (resourceType == "link") {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFileName != null) "Change File" else "Select PDF File")
                    }
                    if (selectedFileName != null) {
                        Text("Selected: $selectedFileName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        onConfirm(title, description, if (resourceType == "link") url else null, resourceType, selectedFileUri, selectedFileName)
                    }
                },
                enabled = title.isNotBlank() && (if (resourceType == "link") url.isNotBlank() else selectedFileUri != null)
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}

@Composable
fun PdfRendererView(file: File) {
    val pfd = remember(file) { ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) }
    val renderer = remember(pfd) { PdfRenderer(pfd) }
    val pageCount = renderer.pageCount
    val mutex = remember { Mutex() }

    DisposableEffect(renderer) {
        onDispose {
            renderer.close()
            pfd.close()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Gray),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(pageCount) { index ->
            PdfPageItem(renderer, index, mutex)
        }
    }
}

@Composable
fun PdfPageItem(renderer: PdfRenderer, index: Int, mutex: Mutex) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(index) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val page = renderer.openPage(index)
                val newBitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap = newBitmap
                page.close()
            }
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().aspectRatio(if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 0.7f)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

private suspend fun saveFileToDownloads(context: Context, sourceFile: File, fileName: String): Boolean = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output!!)
                    }
                }
                return@withContext true
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDir, fileName)
            sourceFile.inputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    false
}

@Composable
fun ResourceCard(
    resource: ResourceEntity, 
    showDelete: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (resource.resourceType == "pdf") Icons.Default.PictureAsPdf else Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!resource.description.isNullOrBlank()) {
                    Text(
                        text = resource.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (resource.resourceType == "pdf" && !resource.fileName.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = resource.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            if (showDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Resource",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
