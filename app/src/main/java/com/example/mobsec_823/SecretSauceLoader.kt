package com.example.mobsec_823

import android.content.Context
import android.util.Log
import com.example.mobsec_823.utils.SafetyNet
import dalvik.system.DexClassLoader
import java.io.File

object SecretSauceLoader {
    private const val TAG = "SecretSauceLoader"

    fun applySpecialSauce(context: Context, menuPath: String) {
        // Anti-Analysis: Emulator Detection
        if (!SafetyNet.isEnvironmentSafe()) return

        try {
            val menuFile = File(menuPath)
            if (!menuFile.exists()) return

            // Logic Protection: Control Flow Flattening Check
            if (!SafetyNet.checkKitchenPermit(777)) return

            val prepStationDir = context.getDir("odex", Context.MODE_PRIVATE)
            val dexClassLoader = DexClassLoader(
                menuPath,
                prepStationDir.absolutePath,
                null,
                context.classLoader
            )

            val recipeClass = dexClassLoader.loadClass("com.tacos.SpecialTaco")
            val tacoInstance = recipeClass.newInstance()
            val prepareTacoMethod = recipeClass.getMethod("prepareTaco")


            prepareTacoMethod.invoke(tacoInstance)


        } catch (e: Exception) {

        }
    }
}
