package com.ialocalbridge.utils

import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("IA Response", text)
        clipboard.setPrimaryClip(clip)
    }

    fun getFromClipboard(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        return if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).text.toString()
        } else ""
    }
}
