package com.ialocalbridge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.ialocalbridge.models.ProviderCoordinates

class CalibrationOverlayManager(
    private val context: Context,
    private val onCalibrationFinished: (ProviderCoordinates) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val currentCoords = ProviderCoordinates()
    private var step = 0 // 0: TextField, 1: SendButton, 2: CopyButton

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    fun show() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView = LayoutInflater.from(context).inflate(R.layout.calibration_overlay, null)
        val instructionTxt = overlayView!!.findViewById<TextView>(R.id.txt_calibration_instruction)
        val btnCancel = overlayView!!.findViewById<Button>(R.id.btn_cancel_calibration)

        overlayView!!.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                saveStep(event.rawX, event.rawY, instructionTxt)
                true
            } else false
        }

        btnCancel.setOnClickListener { hide() }
        windowManager.addView(overlayView, params)
    }

    private fun saveStep(x: Float, y: Float, textView: TextView) {
        when (step) {
            0 -> {
                currentCoords.textFieldX = x
                currentCoords.textFieldY = y
                step++
                textView.text = "Cliquez sur : LE BOUTON ENVOYER"
            }
            1 -> {
                currentCoords.sendButtonX = x
                currentCoords.sendButtonY = y
                step++
                textView.text = "Cliquez sur : LE BOUTON COPIER (Dernier message)"
            }
            2 -> {
                currentCoords.copyButtonX = x
                currentCoords.copyButtonY = y
                onCalibrationFinished(currentCoords)
                hide()
                Toast.makeText(context, "Calibration terminée !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hide() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}
