package com.ialocalbridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputMethodManager

class ClickAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ClickAccessibilityService? = null
        private const val TAG = "ClickAccessibilityService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service Connected")
    }

    // Classe pour stocker l'identité technique d'un bouton
    data class NodeSignature(
        val resourceId: String?,
        val className: String?,
        val description: String?,
        val isClickable: Boolean
    )

    // Capture la signature technique de ce qu'il y a aux coordonnées X/Y
    fun getNodeSignature(x: Float, y: Float): NodeSignature? {
        val node = findNodeAt(x, y)
        return if (node != null) {
            val sig = NodeSignature(
                node.viewIdResourceName,
                node.className?.toString(),
                node.contentDescription?.toString(),
                node.isClickable
            )
            Log.d(TAG, "Captured signature at ($x, $y): $sig")
            node.recycle()
            sig
        } else {
            Log.w(TAG, "No node found at ($x, $y) to capture signature.")
            null
        }
    }

    // Vérifie si l'élément aux coordonnées X/Y correspond à la signature cible
    fun compareSignature(x: Float, y: Float, target: NodeSignature): Boolean {
        val current = getNodeSignature(x, y) ?: return false
        
        // On compare l'ID et la classe (les plus fiables)
        return if (target.resourceId != null && current.resourceId != null) {
            target.resourceId == current.resourceId && target.className == current.className
        } else {
            target.className == current.className && target.description == current.description
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Obligatoire pour AccessibilityService
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // --- Element Finding Methods ---

    fun findNodeAt(x: Float, y: Float): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeAtRecursive(rootNode, x.toInt(), y.toInt())
    }

    private fun findNodeAtRecursive(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        if (rect.contains(x, y)) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val result = findNodeAtRecursive(child, x, y)
                if (result != null) return result
            }
            return AccessibilityNodeInfo.obtain(node)
        }
        return null
    }

    fun getLastVisibleText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val texts = mutableListOf<Pair<String, Int>>()
        findAllTextsRecursive(rootNode, texts)
        
        // On prend le texte du nœud dont le bas (bottom) est le plus élevé sur l'écran
        return texts.maxByOrNull { it.second }?.first ?: ""
    }

    private fun findAllTextsRecursive(node: AccessibilityNodeInfo, texts: MutableList<Pair<String, Int>>) {
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        if (text.isNotEmpty() && text.length > 1) {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            texts.add(text to rect.bottom)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findAllTextsRecursive(child, texts)
        }
    }

    fun findLastNodeByKeywords(keywords: List<String>): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val matches = mutableListOf<AccessibilityNodeInfo>()
        findNodesRecursiveWithKeywords(rootNode, keywords, matches)
        
        return if (matches.isNotEmpty()) {
            val lastNode = matches.maxByOrNull { 
                val rect = android.graphics.Rect()
                it.getBoundsInScreen(rect)
                rect.bottom 
            }
            matches.forEach { if (it != lastNode) it.recycle() }
            lastNode
        } else null
    }

    private fun findNodesRecursiveWithKeywords(node: AccessibilityNodeInfo, keywords: List<String>, matches: MutableList<AccessibilityNodeInfo>) {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        
        if (keywords.any { text.contains(it, ignoreCase = true) || desc.contains(it, ignoreCase = true) }) {
            matches.add(AccessibilityNodeInfo.obtain(node))
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesRecursiveWithKeywords(child, keywords, matches)
        }
    }

    // --- Action Methods ---

    fun clickAt(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 800))
        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        return if (node != null && node.isClickable) {
            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            success
        } else {
            node?.recycle()
            false
        }
    }

    fun pasteText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editableNode = findEditableNodeRecursive(rootNode)
        return if (editableNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val success = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            editableNode.recycle()
            success
        } else false
    }

    private fun findEditableNodeRecursive(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isEditable || root.className?.contains("EditText", true) == true) {
            return AccessibilityNodeInfo.obtain(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findEditableNodeRecursive(child)
            if (result != null) return result
        }
        return null
    }

    fun closeKeyboard() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
