package com.example.mobsec_823.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.mobsec_823.data.DatabaseHelper
import com.example.mobsec_823.data.UserLocation
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@Composable
fun LocationSharingScreen(userId: Int, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var locations by remember { mutableStateOf<List<UserLocation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("Pull down to refresh") }

    // Helper to open Google Maps
    val openInMaps = { lat: Double, lng: Double, label: String ->
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(mapIntent)
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            statusMessage = "Permission granted. Fetching location..."
        }
    }

    // Function to get and send location
    val shareLocation = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    scope.launch {
                        val success = DatabaseHelper.updateLocation(
                            userId, location.latitude, location.longitude
                        )
                        statusMessage = if (success) "Location shared!" else "Failed to share."
                        locations = DatabaseHelper.getAllLocations() // Refresh list
                    }
                } else {
                    statusMessage = "Could not get location. Is GPS on?"
                }
            }
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        locations = DatabaseHelper.getAllLocations()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Location Sharing", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // My Sharing Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Share your current position with others", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { shareLocation() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Share My Location")
                }
                Text(statusMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Other Users", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Tap a user to see them on Google Maps", fontSize = 12.sp, color = Color.Gray)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(locations) { loc ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, Color.LightGray),
                        onClick = { openInMaps(loc.latitude, loc.longitude, loc.username ?: "User ${loc.userId}") }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val displayName = loc.username ?: "User ${loc.userId}"
                                Text(
                                    text = if (loc.userId == userId) "$displayName (You)" else displayName,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Last seen: ${loc.lastSeen ?: "Unknown"}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("View Map", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
