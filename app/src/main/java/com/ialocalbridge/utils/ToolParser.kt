package com.ialocalbridge.utils

import org.json.JSONArray
import org.json.JSONObject

object ToolParser {
    private const val TOOL_CALL_START = "<|tool_calls_begin|>"
    private const val TOOL_CALL_END = "<|tool_calls_end|>"
    private var callIdCounter = 0L

    data class ParsedToolCall(
        val id: String,
        val name: String,
        val arguments: String
    )

    data class ToolCallResult(
        val toolCalls: List<ParsedToolCall>,
        val remainingText: String
    )

    private fun nextCallId(): String {
        callIdCounter++
        return "call_$callIdCounter"
    }

    fun parseToolCalls(text: String): ToolCallResult? {
        val normalized = text.replace('\uff5c', '|').replace('\u2581', '_')
        val startIdx = findTagStart(normalized)
        if (startIdx < 0) return null
        val endIdx = normalized.indexOf(TOOL_CALL_END, startIdx)
        if (endIdx < 0) return null
        val inner = normalized.substring(startIdx + TOOL_CALL_START.length, endIdx)
        val calls = extractCalls(inner)
        if (calls.isEmpty()) return null
        val remaining = normalized.substring(0, startIdx) + normalized.substring(endIdx + TOOL_CALL_END.length)
        return ToolCallResult(calls, remaining)
    }

    fun repairJsonString(raw: String): String {
        var s = repairInvalidBackslashes(raw)
        s = repairUnquotedKeys(s)
        s = balanceBrackets(s)
        return s
    }

    private fun findTagStart(text: String): Int {
        val variants = listOf(TOOL_CALL_START, "<tool_calls>")
        var best = -1
        for (v in variants) {
            val idx = text.indexOf(v)
            if (idx >= 0 && (best < 0 || idx < best)) best = idx
        }
        return best
    }

    private fun extractCalls(inner: String): List<ParsedToolCall> {
        val calls = mutableListOf<ParsedToolCall>()
        if (inner.contains("<invoke ")) {
            calls.addAll(extractInvokeCalls(inner))
        }
        if (calls.isEmpty()) {
            calls.addAll(extractJsonCalls(inner))
        }
        if (calls.isEmpty()) {
            calls.addAll(extractXmlCalls(inner))
        }
        return calls
    }

    private fun extractInvokeCalls(inner: String): List<ParsedToolCall> {
        val calls = mutableListOf<ParsedToolCall>()
        val invokePattern = "<invoke name=\"([^\"]+)\">([\\s\\S]*?)</invoke>"
        val regex = Regex(invokePattern)
        regex.findAll(inner).forEach { match ->
            val name = match.groupValues[1]
            val rawArgs = match.groupValues[2].trim()
            val args = try {
                JSONObject(rawArgs).toString()
            } catch (e: Exception) {
                repairJsonString(rawArgs)
            }
            calls.add(ParsedToolCall(nextCallId(), name, args))
        }
        return calls
    }

    private fun extractJsonCalls(inner: String): List<ParsedToolCall> {
        val calls = mutableListOf<ParsedToolCall>()
        val trimmed = inner.trim()
        if (trimmed.startsWith("[")) {
            try {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", obj.optString("function", ""))
                    val args = if (obj.has("arguments")) {
                        val a = obj.get("arguments")
                        if (a is String) a else a.toString()
                    } else "{}"
                    calls.add(ParsedToolCall(nextCallId(), name, args))
                }
            } catch (e: Exception) {
                val repaired = repairJsonString(trimmed)
                try {
                    val arr = JSONArray(repaired)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optString("name", "")
                        val args = obj.optString("arguments", "{}")
                        calls.add(ParsedToolCall(nextCallId(), name, args))
                    }
                } catch (_: Exception) {}
            }
        } else if (trimmed.startsWith("{")) {
            try {
                val obj = JSONObject(trimmed)
                val name = obj.optString("name", "")
                val args = obj.optString("arguments", "{}")
                calls.add(ParsedToolCall(nextCallId(), name, args))
            } catch (_: Exception) {}
        }
        return calls
    }

    private fun extractXmlCalls(inner: String): List<ParsedToolCall> {
        val calls = mutableListOf<ParsedToolCall>()
        val xmlPattern = "<tool_call>\\s*<name>([\\s\\S]*?)</name>\\s*<arguments>([\\s\\S]*?)</arguments>\\s*</tool_call>"
        val regex = Regex(xmlPattern)
        regex.findAll(inner).forEach { match ->
            val name = match.groupValues[1].trim()
            val rawArgs = match.groupValues[2].trim()
            val args = try {
                JSONObject(rawArgs).toString()
            } catch (e: Exception) {
                repairJsonString(rawArgs)
            }
            calls.add(ParsedToolCall(nextCallId(), name, args))
        }
        return calls
    }

    private fun repairInvalidBackslashes(s: String): String {
        val sb = StringBuilder()
        var i = 0
        val validEscapes = setOf('"', '\\', '/', 'b', 'f', 'n', 'r', 't', 'u')
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val next = s[i + 1]
                if (next in validEscapes) {
                    sb.append('\\').append(next)
                    i += 2
                } else {
                    sb.append("\\\\").append(next)
                    i += 2
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun repairUnquotedKeys(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            if ((s[i] == '{' || s[i] == ',') && i + 1 < s.length) {
                sb.append(s[i])
                i++
                while (i < s.length && s[i].isWhitespace()) { sb.append(s[i]); i++ }
                if (i < s.length && (s[i].isLetter() || s[i] == '_')) {
                    val keyStart = i
                    while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++
                    if (i < s.length && s[i] == ':') {
                        sb.append('"').append(s.substring(keyStart, i)).append('"')
                    } else {
                        sb.append(s.substring(keyStart, i))
                    }
                }
            } else {
                sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun balanceBrackets(s: String): String {
        var depth = 0
        var inString = false
        var escaped = false
        for (c in s) {
            if (escaped) { escaped = false; continue }
            if (c == '\\') { escaped = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (inString) continue
            if (c == '{' || c == '[') depth++
            if (c == '}' || c == ']') depth--
        }
        var result = s
        while (depth > 0) {
            result += if (result.contains('[') && !result.contains(']')) "]" else "}"
            depth--
        }
        return result
    }

    fun buildToolCallPrompt(tools: JSONArray): String {
        val sb = StringBuilder()
        sb.append("You have access to the following tools:\n\n")
        for (i in 0 until tools.length()) {
            val tool = tools.getJSONObject(i)
            val func = tool.optJSONObject("function") ?: tool
            val name = func.optString("name", "tool_$i")
            val desc = func.optString("description", "")
            val params = func.optJSONObject("parameters")?.toString(2) ?: "{}"
            sb.append("## $name\n")
            sb.append("Description: $desc\n")
            sb.append("Parameters: $params\n\n")
        }
        sb.append("To call a tool, use the following format:\n")
        sb.append(TOOL_CALL_START)
        sb.append("\n")
        sb.append("[{\"name\": \"tool_name\", \"arguments\": {...}}]\n")
        sb.append(TOOL_CALL_END)
        sb.append("\n")
        return sb.toString()
    }
}
