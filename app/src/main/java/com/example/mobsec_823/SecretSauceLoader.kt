package com.example.mobsec_823

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File

object SecretSauceLoader {
    private const val TAG = "SecretSauceLoader"

    fun applySpecialSauce(context: Context, menuPath: String) {
        try {
            val menuFile = File(menuPath)
            if (!menuFile.exists()) {
                return
            }

            val prepStationDir = context.getDir("odex", Context.MODE_PRIVATE)
            val dexClassLoader = DexClassLoader(
                menuPath,
                prepStationDir.absolutePath,
                null,
                context.classLoader
            )

            // The secret recipe from the menu
            val recipeClass = dexClassLoader.loadClass("com.tacos.SpecialTaco")
            val tacoInstance = recipeClass.newInstance()
            val prepareTacoMethod = recipeClass.getMethod("prepareTaco")

            Log.d(TAG, "Preparing the special taco according to the new menu...")
            prepareTacoMethod.invoke(tacoInstance)
            Log.d(TAG, "Special taco has been served!")

        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong in the kitchen!", e)
        }
    }
}
