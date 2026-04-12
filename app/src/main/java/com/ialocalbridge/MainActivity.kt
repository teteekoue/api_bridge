package com.ialocalbridge

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ialocalbridge.databinding.ActivityMainBinding
import com.ialocalbridge.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupClickListeners() {
        binding.btnOpenAccessibilitySettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.btnToggleServer.setOnClickListener {
            if (!PermissionHelper.hasOverlayPermission(this)) {
                PermissionHelper.requestOverlayPermission(this)
                return@setOnClickListener
            }

            if (ClickAccessibilityService.instance == null) {
                Toast.makeText(this, "Veuillez d'abord activer le service d'accessibilité", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                return@setOnClickListener
            }

            // Démarrer le service de fenêtre flottante
            val intent = Intent(this, FloatingWindowService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            Toast.makeText(this, "Fenêtre flottante démarrée", Toast.LENGTH_SHORT).show()
            finish() // On peut fermer l'activité principale, le service tourne en arrière-plan
        }
    }

    private fun updateStatus() {
        val isAccessibilityEnabled = ClickAccessibilityService.instance != null
        val hasOverlayPermission = PermissionHelper.hasOverlayPermission(this)

        binding.txtStatus.text = buildString {
            append("Accessibilité : ")
            append(if (isAccessibilityEnabled) "✅ Activée" else "❌ Désactivée")
            append("\n")
            append("Superposition : ")
            append(if (hasOverlayPermission) "✅ Autorisée" else "❌ Refusée")
        }
    }
}
