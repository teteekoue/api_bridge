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

    fun forceScrollToBottom() {
        val rootNode = rootInActiveWindow ?: return
        val scrollableNode = findScrollableNode(rootNode)
        if (scrollableNode != null) {
            for (i in 0..3) {
                scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
            scrollableNode.recycle()
        } else {
            val path = Path()
            val metrics = resources.displayMetrics
            val centerX = metrics.widthPixels / 2f
            val startY = metrics.heightPixels * 0.9f
            val endY = metrics.heightPixels * 0.1f
            path.moveTo(centerX, startY)
            path.lineTo(centerX, endY)
            val builder = GestureDescription.Builder()
            builder.addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            dispatchGesture(builder.build(), null, null)
        }
    }

    fun pasteText(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) 
            ?: findEditableNode(rootNode)
        
        if (focusedNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
        }
    }

    fun isNodeEnabledAt(x: Float, y: Float): Boolean {
        val rootNode = rootInActiveWindow ?: return true
        val node = findNodeAtPoint(rootNode, x.toInt(), y.toInt())
        val isEnabled = node?.isEnabled ?: true
        node?.recycle()
        return isEnabled
    }

    private fun findNodeAtPoint(root: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = android.graphics.Rect()
        root.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findNodeAtPoint(child, x, y)
            if (result != null) return result
        }
        return AccessibilityNodeInfo.obtain(root)
    }

    fun findAndFocusEditable(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editable = findEditableNode(rootNode)
        return if (editable != null) {
            editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            editable.recycle()
            true
        } else false
    }

    private fun findEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable || root.className?.contains("EditText", true) == true) {
            return AccessibilityNodeInfo.obtain(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findEditableNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return AccessibilityNodeInfo.obtain(root)
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findScrollableNode(child)
            if (result != null) return result
        }
        return null
    }
}
