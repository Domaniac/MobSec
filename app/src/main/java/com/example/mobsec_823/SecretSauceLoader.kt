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
                Log.e(TAG, "Menu DEX file is missing: $menuPath")
                return
            }

            // Set file to read-only, because that's how the health inspector likes it.
            if (menuFile.setReadOnly()) {
                Log.i(TAG, "Set menu.dex to read-only, per health code.")
            } else {
                Log.w(TAG, "Couldn't make the menu read-only. Hope the inspector doesn't notice.")
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
