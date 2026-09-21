package com.example.digitallevel

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class OrientationService : Service() {

    companion object {
        const val ACTION_ORIENTATION = "com.example.digitallevel.ORIENTATION_UPDATE"
        private const val CHANNEL_ID = "orientation_channel"
        private const val NOTIF_ID = 2
    }

    private var binderService: IBinder? = null
    private var isRunning = false
    private lateinit var serviceScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, createNotification("Conectando ao daemon de orientação..."))
        connectToDaemon()
        return START_STICKY
    }

    private fun connectToDaemon() {
        serviceScope.launch {
            try {
                val serviceManager = Class.forName("android.os.ServiceManager")
                val getService = serviceManager.getMethod("getService", String::class.java)
                val binder = getService.invoke(null, "hatsens_orientation") as IBinder?
                if (binder != null) {
                    binderService = binder
                    isRunning = true
                    startReading()
                } else {
                    updateNotification("Erro: serviço hatsens_orientation não encontrado")
                    Log.e("OrientationService", "Daemon não está rodando")
                }
            } catch (e: Exception) {
                Log.e("OrientationService", "Falha na conexão", e)
                updateNotification("Erro: ${e.message}")
            }
        }
    }

    private suspend fun startReading() {
        while (isRunning && binderService != null) {
            try {
                val roll = transact(1)  // código 1 = getRoll
                val pitch = transact(2) // código 2 = getPitch

                updateNotification(String.format("R: %.1f°  P: %.1f°", roll, pitch))

                val intent = Intent(ACTION_ORIENTATION).apply {
                    setPackage(packageName)
                    putExtra("roll", roll)
                    putExtra("pitch", pitch)
                }
                sendBroadcast(intent)

                delay(100) // 10 Hz
            } catch (e: RemoteException) {
                Log.e("OrientationService", "Erro na chamada remota", e)
                break
            } catch (e: Exception) {
                Log.e("OrientationService", "Erro inesperado", e)
                break
            }
        }
    }

    private fun transact(code: Int): Float {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken("android.os.IBinder") // qualquer token, não é verificado
            binderService?.transact(code, data, reply, 0)
            reply.readFloat()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nível Digital")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canal do Nível Digital",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }
}