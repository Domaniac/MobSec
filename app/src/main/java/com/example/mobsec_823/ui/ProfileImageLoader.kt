package com.example.mobsec_823.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ProfileImageLoader"

/**
 * Composable helper that decodes a base64-encoded profile image (from LONGBLOB).
 * Returns a Bitmap? that updates when the base64 string changes.
 */
@Composable
fun rememberProfileBitmap(profileImageBase64: String?): Bitmap? {
    var bitmap by remember(profileImageBase64) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(profileImageBase64) {
        bitmap = loadProfileBitmap(profileImageBase64)
    }

    return bitmap
}

/**
 * Decodes a base64-encoded profile image string into a Bitmap.
 */
fun decodeBase64ProfileImage(base64String: String?): Bitmap? {
    if (base64String.isNullOrBlank()) return null
    return try {
        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to decode base64 profile image", e)
        null
    }
}

/**
 * Loads a profile image bitmap from a base64-encoded string (from LONGBLOB).
 * Should be called from a coroutine (suspend function).
 */
suspend fun loadProfileBitmap(profileImageBase64: String?): Bitmap? {
    if (profileImageBase64.isNullOrBlank()) return null

    return withContext(Dispatchers.IO) {
        try {
            val imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load profile image from base64", e)
            null
        }
    }
}