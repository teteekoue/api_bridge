package com.ialocalbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ialocalbridge.databinding.ActivityMainBinding
import com.ialocalbridge.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val calibrationManager by lazy { CalibrationManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupClickListeners()
        setupNavigation()
        checkPermissions()
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_config -> {
                    binding.layoutConfig.visibility = View.VISIBLE
                    binding.layoutHelp.visibility = View.GONE
                    true
                }
                R.id.nav_help -> {
                    binding.layoutConfig.visibility = View.GONE
                    binding.layoutHelp.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        refreshCalibrationList()
    }

    private fun setupSpinners() {
        // Modes de connexion
        val modes = arrayOf("WiFi (Réseau Local)", "USB (ADB Reverse)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        binding.spinnerMode.adapter = adapter
        
        binding.spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                binding.txtUsbHint.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        refreshCalibrationList()
    }

    private fun refreshCalibrationList() {
        val calibrations = calibrationManager.getAllProviderNames().toMutableList()
        if (calibrations.isEmpty()) calibrations.add("Aucune calibration trouvée")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, calibrations)
        binding.spinnerCalibration.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnGrantPermissions.setOnClickListener {
            if (!PermissionHelper.hasOverlayPermission(this)) {
                PermissionHelper.requestOverlayPermission(this)
            } else if (ClickAccessibilityService.instance == null) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        binding.btnDeleteCalibration.setOnClickListener {
            val selected = binding.spinnerCalibration.selectedItem?.toString()
            if (selected != null && selected != "Aucune calibration trouvée") {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Supprimer le profil")
                    .setMessage("Voulez-vous vraiment supprimer '$selected' ?")
                    .setPositiveButton("Supprimer") { _, _ ->
                        calibrationManager.deleteProvider(selected)
                        refreshCalibrationList()
                        Toast.makeText(this, "Profil supprimé", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        }

        binding.btnStartServer.setOnClickListener {
            val portStr = binding.edtPort.text.toString()
            val port = if (portStr.isNotEmpty()) portStr.toInt() else 8080
            val selectedCalibration = binding.spinnerCalibration.selectedItem?.toString() ?: "default_provider"

            if (selectedCalibration == "Aucune calibration trouvée") {
                Toast.makeText(this, "Veuillez d'abord calibrer une application", Toast.LENGTH_LONG).show()
            }

            // Sauvegarder le profil actuel pour AutomationCoordinator
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .putString("current_provider", selectedCalibration)
                .apply()

            // Démarrer le service avec les extras
            val intent = Intent(this, FloatingWindowService::class.java).apply {
                putExtra("port", port)
                putExtra("calibration_name", selectedCalibration)
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            Toast.makeText(this, "NEMAPI Bridge activé sur le port $port", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true) // L'appli reste active en fond mais se "ferme" visuellement
        }
    }

    private fun checkPermissions() {
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)
        val hasAccessibility = ClickAccessibilityService.instance != null

        if (hasOverlay && hasAccessibility) {
            binding.cardPermissions.visibility = View.GONE
            binding.cardConfig.visibility = View.VISIBLE
        } else {
            binding.cardPermissions.visibility = View.VISIBLE
            binding.cardConfig.visibility = View.GONE
            
            binding.txtPermissionStatus.text = buildString {
                append("Statut :\n")
                append("- Superposition : ${if (hasOverlay) "✅" else "❌"}\n")
                append("- Accessibilité : ${if (hasAccessibility) "✅" else "❌"}")
            }
        }
    }
}
