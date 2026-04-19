package com.ialocalbridge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.ialocalbridge.R
import com.ialocalbridge.models.ProviderCoordinates

class CalibrationOverlayManager(
    private val context: Context,
    private val onCalibrationFinished: (ProviderCoordinates) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val currentCoords = ProviderCoordinates()
    private var step = 0 // 0: TextField, 1: SendButton, 2: ScrollDown, 3: CopyButton

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    fun show() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // FLAG_NOT_TOUCH_MODAL : crucial pour pouvoir toucher l'appli à côté de la zone
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView = LayoutInflater.from(context).inflate(R.layout.calibration_overlay, null)
        val instructionTxt = overlayView!!.findViewById<TextView>(R.id.txt_calibration_instruction)
        val btnCancel = overlayView!!.findViewById<Button>(R.id.btn_cancel_calibration)
        val calibrationZone = overlayView!!.findViewById<FrameLayout>(R.id.calibration_zone)
        val dragHandle = overlayView!!.findViewById<ImageView>(R.id.zone_drag_handle)

        // Gestion du déplacement du rectangle via la poignée
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    calibrationZone.translationX += (event.rawX - initialTouchX)
                    calibrationZone.translationY += (event.rawY - initialTouchY)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                else -> false
            }
        }

        // Clic à l'intérieur du rectangle pour calibrer
        calibrationZone.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // On enregistre la position absolue sur l'écran
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
                textView.text = "Étape 2 : Cliquez sur LE BOUTON ENVOYER\n(Placez la zone rouge dessus avant)"
            }
            1 -> {
                currentCoords.sendButtonX = x
                currentCoords.sendButtonY = y
                step++
                textView.text = "Étape 3 : Cliquez sur LE BOUTON DE BAS\n(Placez la zone rouge dessus avant)"
            }
            2 -> {
                currentCoords.scrollDownButtonX = x
                currentCoords.scrollDownButtonY = y
                step++
                textView.text = "Étape 4 : Cliquez sur LE BOUTON COPIER\n(Placez la zone rouge dessus avant)"
            }
            3 -> {
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
            if (it.windowToken != null) {
                windowManager.removeView(it)
            }
            overlayView = null
        }
    }
}
