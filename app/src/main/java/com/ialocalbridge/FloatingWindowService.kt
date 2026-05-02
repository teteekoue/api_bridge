package com.ialocalbridge

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.ialocalbridge.R

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var floatingView: View
    private var apiServer: LocalApiServer? = null
    private var isServerRunning = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    private var selectedCalibration = "default_provider"
    private var selectedPort = 8080

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        selectedPort = intent?.getIntExtra("port", 8080) ?: 8080
        selectedCalibration = intent?.getStringExtra("calibration_name") ?: "default_provider"
        
        startForegroundService()
        
        if (!isServerRunning) {
            startServer()
        }
        
        return START_STICKY
    }

    private fun startServer() {
        try {
            apiServer?.stop()
            apiServer = LocalApiServer(selectedPort, this)
            apiServer?.start()
            isServerRunning = true
            updateNotification("NEMAPI actif : port $selectedPort (Profil: $selectedCalibration)")
            
            if (::floatingView.isInitialized) {
                val btnStartApi = floatingView.findViewById<Button>(R.id.btn_start_api)
                btnStartApi.text = "API: ON"
                btnStartApi.setBackgroundColor(Color.GREEN)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur serveur: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startForegroundService() {
        val channelId = "api_bridge_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "NEMAPI Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("NEMAPI Bridge est actif")
            .setContentText("Le serveur API tourne sur le port $selectedPort")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("InflateParams")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_control_window, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager.addView(floatingView, params)
        setupDragAndButtons()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragAndButtons() {
        val btnDrag = floatingView.findViewById<ImageView>(R.id.btn_drag)
        val btnClose = floatingView.findViewById<ImageView>(R.id.btn_close)
        val btnCalibrate = floatingView.findViewById<Button>(R.id.btn_calibrate)
        val btnStartApi = floatingView.findViewById<Button>(R.id.btn_start_api)

        if (isServerRunning) {
            btnStartApi.text = "API: ON"
            btnStartApi.setBackgroundColor(Color.GREEN)
        }

        btnDrag.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }

        btnClose.setOnClickListener { stopSelf() }

        btnCalibrate.setOnClickListener {
            val overlay = CalibrationOverlayManager(this) { _ ->
                // La sauvegarde du nom se fait dans l'overlay manager
            }
            overlay.show()
        }

        btnStartApi.setOnClickListener {
            if (!isServerRunning) {
                startServer()
            } else {
                AutomationCoordinator.stopCurrentJob() // Arrêter l'automatisation en cours
                apiServer?.stop()
                isServerRunning = false
                btnStartApi.text = "API: OFF"
                btnStartApi.setBackgroundColor(Color.RED)
                updateNotification("NEMAPI est arrêté")
            }
        }
    }

    private fun updateNotification(text: String) {
        val channelId = "api_bridge_service"
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("NEMAPI Bridge")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        apiServer?.stop()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
