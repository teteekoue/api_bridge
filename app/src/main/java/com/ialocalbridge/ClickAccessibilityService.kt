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
        // Note: l'ID peut être nul dans certaines applis, on compare alors la classe et la description
        return if (target.resourceId != null && current.resourceId != null) {
            target.resourceId == current.resourceId && target.className == current.className
        } else {
            target.className == current.className && target.description == current.description
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility Service Destroyed")
        instance = null
    }

    // --- Element Finding Methods (Public) ---

    // Make this public and parameterless, searches within rootInActiveWindow
    fun findEditableNode(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findEditableNodeRecursive(rootNode)
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

    private fun findNodeByText(searchText: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeRecursive(rootNode, { node ->
            node.text?.toString()?.contains(searchText, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.contains(searchText, ignoreCase = true) == true
        })
    }

    private fun findNodeByDescription(searchDesc: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeRecursive(rootNode, { node ->
            node.contentDescription?.toString()?.contains(searchDesc, ignoreCase = true) == true
        })
    }
    
    // Public method to find send button
    fun findSendButtonNode(): AccessibilityNodeInfo? {
        Log.d(TAG, "Searching for send button...")
        val rootNode = rootInActiveWindow ?: return null
        val potentialSendTexts = listOf("Send", "Envoyer", "Submit", "Ask", "Go", "Search")
        val potentialSendDescs = listOf("Send message", "Send", "Submit", "Ask")

        for (text in potentialSendTexts) {
            findNodeByText(text)?.let { Log.d(TAG, "Found send button by text: '$text'"); return it }
        }
        for (desc in potentialSendDescs) {
            findNodeByDescription(desc)?.let { Log.d(TAG, "Found send button by description: '$desc'"); return it }
        }
        Log.w(TAG, "Send button not found using common texts/descriptions.")
        return null
    }

    // Public method to find copy button
    fun findCopyButtonNode(): AccessibilityNodeInfo? {
        Log.d(TAG, "Searching for copy button...")
        val rootNode = rootInActiveWindow ?: return null
        val potentialCopyTexts = listOf("Copy", "Copier", "Copy Response")
        val potentialCopyDescs = listOf("Copy message", "Copy")
        
        for (text in potentialCopyTexts) {
            findNodeByText(text)?.let { Log.d(TAG, "Found copy button by text: '$text'"); return it }
        }
        for (desc in potentialCopyDescs) {
            findNodeByDescription(desc)?.let { Log.d(TAG, "Found copy button by description: '$desc'"); return it }
        }
        Log.w(TAG, "Copy button not found using common texts/descriptions.")
        return null
    }

    // Public method to find stop button
    fun findStopButtonNode(): AccessibilityNodeInfo? {
        Log.d(TAG, "Searching for stop button...")
        val potentialStopTexts = listOf("Stop", "Cancel", "Abort", "Annuler", "Arrêter")
        val potentialStopDescs = listOf("Stop generation", "Cancel generation", "Interrupt")
        
        val rootNode = rootInActiveWindow ?: return null
        
        for (text in potentialStopTexts) {
            findNodeByText(text)?.let { Log.d(TAG, "Found stop button by text: '$text'"); return it }
        }
        for (desc in potentialStopDescs) {
            findNodeByDescription(desc)?.let { Log.d(TAG, "Found stop button by description: '$desc'"); return it }
        }
        Log.d(TAG, "Stop button not found.")
        return null
    }
    
    // Public method to check if generation is active by looking at specific coordinates
    fun isGenerationActiveAt(x: Float, y: Float): Boolean {
        val node = findNodeAt(x, y)
        if (node != null) {
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val className = node.className?.toString() ?: ""
            
            val stopKeywords = listOf("Stop", "Arrêter", "Cancel", "Annuler", "Generating", "En cours")
            val isActive = stopKeywords.any { 
                text.contains(it, ignoreCase = true) || desc.contains(it, ignoreCase = true) 
            }
            
            if (isActive) {
                Log.d(TAG, "Generation detected at coords via text/desc: $text | $desc")
                node.recycle()
                return true
            }
            node.recycle()
        }
        
        // Backup: still check globally if something was missed
        return isGenerationActive()
    }

    // Public method to check if generation is currently active globally
    fun isGenerationActive(): Boolean {
        val stopNode = findStopButtonNode()
        if (stopNode != null) {
            Log.d(TAG, "Stop button found, generation is active.")
            stopNode.recycle()
            return true
        }
        return false
    }

    fun findNodeAt(x: Float, y: Float): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeAtRecursive(rootNode, x.toInt(), y.toInt())
    }

    private fun findNodeAtRecursive(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        if (rect.contains(x, y)) {
            // Check children first for more specificity
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val result = findNodeAtRecursive(child, x, y)
                if (result != null) return result
            }
            return AccessibilityNodeInfo.obtain(node)
        }
        return null
    }
    
    // --- Action Methods ---

    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        return if (node != null && node.isClickable) {
            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Clicked node: ${node.text ?: node.contentDescription ?: node.className}, Success: $success")
            node.recycle()
            success
        } else {
            Log.w(TAG, "Attempted to click null or non-clickable node.")
            node?.recycle() // Recycle if node is not null but not clickable
            false
        }
    }

    fun clickAt(x: Float, y: Float): Boolean {
        Log.d(TAG, "Attempting to click at coordinates: ($x, $y)")
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        Log.d(TAG, "Performing swipe from ($startX, $startY) to ($endX, $endY)")
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 500))
        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun findLastNodeByKeywords(keywords: List<String>): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val matches = mutableListOf<AccessibilityNodeInfo>()
        findNodesRecursiveWithKeywords(rootNode, keywords, matches)
        
        // On retourne le nœud le plus bas (Y maximum)
        return if (matches.isNotEmpty()) {
            val lastNode = matches.maxByOrNull { 
                val rect = android.graphics.Rect()
                it.getBoundsInScreen(rect)
                rect.bottom 
            }
            // On recycle les autres
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
    
    // pasteText now uses the public findEditableNode()
    fun pasteText(text: String): Boolean {
        val editableNode = findEditableNode() 
        return if (editableNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val success = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "Pasted text into editable node. Success: $success")
            editableNode.recycle()
            success
        } else {
            Log.w(TAG, "No editable node found to paste text.")
            false
        }
    }

    fun closeKeyboard() {
        Log.d(TAG, "Attempting to close keyboard")
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun forceScrollToBottom() {
        Log.d(TAG, "Attempting to force scroll to bottom")
        val rootNode = rootInActiveWindow ?: return
        val scrollableNode = findScrollableNode(rootNode)
        if (scrollableNode != null) {
            for (i in 0..3) {
                scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Thread.sleep(200) // Small delay between scrolls
            }
            scrollableNode.recycle()
        } else {
            // Fallback: swipe manual
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
    
    // --- Helper Methods ---

    private fun findNodeRecursive(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeRecursive(child, predicate)
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
