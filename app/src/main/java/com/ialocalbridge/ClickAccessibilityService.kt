package com.ialocalbridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClickAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ClickAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun clickAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(builder.build(), null, null)
    }

    fun closeKeyboard() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun pasteText(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
        }
    }

    fun scrollDown() {
        val rootNode = rootInActiveWindow ?: return
        // On cherche le premier élément scrollable
        val scrollableNode = findScrollableNode(rootNode)
        if (scrollableNode != null) {
            scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            scrollableNode.recycle()
        } else {
            // Si on ne trouve pas de noeud scrollable, on tente un geste de swipe manuel
            val path = Path()
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
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findScrollableNode(child)
            if (result != null) return result
        }
        return null
    }
}
