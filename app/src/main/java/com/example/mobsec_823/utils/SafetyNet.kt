package com.example.mobsec_823.utils

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import java.io.File

object SafetyNet {
    /**
     * Checks if the environment is a safe physical device.
     * Enhanced Emulator Detection for research purposes.
     */
    fun isEnvironmentSafe(): Boolean {
        val indicators = listOf(
            Build.FINGERPRINT.startsWith("generic"),
            Build.FINGERPRINT.startsWith("unknown"),
            Build.MODEL.contains("google_sdk"),
            Build.MODEL.contains("Emulator"),
            Build.MODEL.contains("Android SDK built for x86"),
            Build.BOARD == "QC_Reference_Phone", // Specific check
            Build.HARDWARE == "goldfish" || Build.HARDWARE == "ranchu",
            Build.MANUFACTURER.contains("Genymotion"),
            Build.PRODUCT.contains("sdk_google") || Build.PRODUCT.contains("google_sdk") || Build.PRODUCT.contains("sdk"),
            File("/dev/socket/qemud").exists(),
            File("/dev/qemu_pipe").exists()
        )

        return !indicators.any { it }
    }

    /**
     * Logic/Time Bomb: Prevents execution unless target conditions are met.
     */
    fun isTriggerArmed(context: Context): Boolean {
        // Target Date: Jan 1, 2025
        val activationDate = 1735689600000L 
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        
        // Target Region: US (for simulation)
        val isCorrectRegion = tm?.simCountryIso?.equals("us", ignoreCase = true) ?: false
        val isTimePassed = System.currentTimeMillis() > activationDate
        
        // In a research scenario, we might return true to allow testing on emulators
        // but for the logic bomb implementation, we check both.
        return isCorrectRegion && isTimePassed
    }

    /**
     * Opaque Predicate: Computationally non-trivial for decompilers but always evaluates to true.
     * x^3 - x is always divisible by 3 for any integer x.
     */
    fun checkOpaquePredicate(x: Int): Boolean {
        return (x * x * x - x) % 3 == 0
    }

    /**
     * Control Flow Flattening for sensitive logic
     */
    fun checkKitchenPermit(permitId: Int): Boolean {
        var state = 0x5D21
        var result = false
        
        while (state != 0) {
            when (state) {
                0x5D21 -> { 
                    state = if (permitId > 0) 0xA1B2 else 0xFFFF
                }
                0xA1B2 -> { 
                    state = if (permitId < 1000) 0x3344 else 0xFFFF
                }
                0x3344 -> { 
                    result = true
                    state = 0 
                }
                0xFFFF -> { 
                    result = false
                    state = 0 
                }
            }
        }
        return result
    }
}
