package com.ialocalbridge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
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
    private var step = 0 // 0: TextField, 1: SendButton, 2: CopyButton
    private var isScrollMode = false

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
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView = LayoutInflater.from(context).inflate(R.layout.calibration_overlay, null)
        val instructionTxt = overlayView!!.findViewById<TextView>(R.id.txt_calibration_instruction)
        val modeTxt = overlayView!!.findViewById<TextView>(R.id.txt_calibration_mode)
        val btnCancel = overlayView!!.findViewById<Button>(R.id.btn_cancel_calibration)
        val btnToggle = overlayView!!.findViewById<Button>(R.id.btn_toggle_mode)
        val rootLayout = overlayView!!.findViewById<FrameLayout>(R.id.calibration_root)

        rootLayout.setOnTouchListener { _, event ->
            if (!isScrollMode && event.action == MotionEvent.ACTION_DOWN) {
                saveStep(event.rawX, event.rawY, instructionTxt)
                true
            } else false
        }

        btnToggle.setOnClickListener {
            isScrollMode = !isScrollMode
            if (isScrollMode) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                rootLayout.setBackgroundColor(Color.TRANSPARENT)
                btnToggle.text = "RETOUR AU CLIC"
                modeTxt.text = "MODE : DÉFILEMENT (MANIPULEZ L'APPLI)"
                modeTxt.setTextColor(Color.GREEN)
                Toast.makeText(context, "Mode défilement activé", Toast.LENGTH_SHORT).show()
            } else {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                rootLayout.setBackgroundColor(Color.parseColor("#66000000"))
                btnToggle.text = "MODE DÉFILEMENT"
                modeTxt.text = "MODE : ENREGISTREMENT (CLIQUEZ SUR L'ÉLÉMENT)"
                modeTxt.setTextColor(Color.parseColor("#FFEB3B"))
            }
            windowManager.updateViewLayout(overlayView, params)
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
                textView.text = "CLIQUEZ SUR : LE BOUTON ENVOYER"
            }
            1 -> {
                currentCoords.sendButtonX = x
                currentCoords.sendButtonY = y
                step++
                textView.text = "CLIQUEZ SUR : LE BOUTON COPIER"
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
            if (it.windowToken != null) {
                windowManager.removeView(it)
            }
            overlayView = null
        }
    }
}
