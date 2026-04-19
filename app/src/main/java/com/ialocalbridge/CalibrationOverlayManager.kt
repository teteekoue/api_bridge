package com.ialocalbridge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.Button
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

        // FLAG_NOT_TOUCH_MODAL permet de toucher l'application derrière si on ne touche pas les vues de l'overlay
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
        val btnNext = overlayView!!.findViewById<Button>(R.id.btn_next_step)
        val target = overlayView!!.findViewById<ImageView>(R.id.calibration_target)

        // Rendre le viseur déplaçable
        target.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val location = IntArray(2)
                    v.getLocationOnScreen(location)
                    initialX = location[0]
                    initialY = location[1]
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    
                    // On déplace le viseur via translation car le parent est un RelativeLayout MATCH_PARENT
                    v.translationX = v.translationX + dx
                    v.translationY = v.translationY + dy
                    
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                else -> false
            }
        }

        btnNext.setOnClickListener {
            saveStep(target, instructionTxt)
        }

        btnCancel.setOnClickListener { hide() }
        windowManager.addView(overlayView, params)
    }

    private fun saveStep(target: View, textView: TextView) {
        val location = IntArray(2)
        target.getLocationOnScreen(location)
        // On prend le centre du viseur
        val centerX = location[0] + target.width / 2f
        val centerY = location[1] + target.height / 2f

        when (step) {
            0 -> {
                currentCoords.textFieldX = centerX
                currentCoords.textFieldY = centerY
                step++
                textView.text = "Placez le viseur sur : LE BOUTON ENVOYER"
            }
            1 -> {
                currentCoords.sendButtonX = centerX
                currentCoords.sendButtonY = centerY
                step++
                textView.text = "Placez le viseur sur : LE BOUTON DE BAS (Défilement)"
            }
            2 -> {
                currentCoords.scrollDownButtonX = centerX
                currentCoords.scrollDownButtonY = centerY
                step++
                textView.text = "Placez le viseur sur : LE BOUTON COPIER"
            }
            3 -> {
                currentCoords.copyButtonX = centerX
                currentCoords.copyButtonY = centerY
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
