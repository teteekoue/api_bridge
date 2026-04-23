package com.ialocalbridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClickAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ClickAccessibilityService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun clickAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(builder.build(), null, null)
    }

    fun swipeUp() {
        val path = Path()
        // Swipe du milieu vers le haut (donc défilement vers le bas)
        val metrics = resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.8f
        val endY = metrics.heightPixels * 0.2f
        path.moveTo(centerX, startY)
        path.lineTo(centerX, endY)
        
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 500))
        dispatchGesture(builder.build(), null, null)
    }

    fun pasteText() {
        val root = rootInActiveWindow ?: return
        val node = findEditableNodeRecursive(root)
        if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            node.recycle()
        }
    }

    fun closeKeyboard() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun findEditableNodeRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNodeRecursive(child)
            if (result != null) return result
        }
        return null
    }

    fun findNodeAt(x: Float, y: Float): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findNodeAtRecursive(root, x.toInt(), y.toInt())
    }

    private fun findNodeAtRecursive(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.contains(x, y)) return null

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeAtRecursive(child, x, y)
            if (found != null) return found
        }
        return node
    }
}
