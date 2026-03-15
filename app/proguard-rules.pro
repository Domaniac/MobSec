# Aggressive Optimization and Obfuscation for Malware Research
# This configuration aims to maximize code confusion for decompilers.

# 1. Aggressive Rename Overloading
-overloadaggressively

# 2. Package Hierarchy Flattening
-repackageclasses 'com.example.mobsec_823.internal'
-allowaccessmodification

# 3. String Obfuscation (General)
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# 4. Entry Points (Essentials)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 5. Reflection Support
-keepclassmembers class java.lang.Runtime {
    public java.lang.Process exec(java.lang.String);
    public static java.lang.Runtime getRuntime();
}

# 6. Compose and UI
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# 7. Data Models
-keepclassmembers class com.example.mobsec_823.data.** { *; }

# 8. MySQL Connector - Fix for R8 missing classes
-dontwarn com.mysql.cj.**
-dontwarn javax.management.**
-dontwarn java.lang.management.**
-dontwarn javax.naming.**
-dontwarn javax.security.sasl.**
-dontwarn javax.security.auth.login.**
-dontwarn javax.security.auth.callback.**
-dontwarn java.sql.**
-dontwarn javax.xml.stream.**
-dontwarn javax.xml.transform.stax.**
-dontwarn com.oracle.bmc.**

# Optional: keep mysql classes if you actually use them at runtime via reflection or similar
-keep class com.mysql.cj.** { *; }

# 9. Optimization passes
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
