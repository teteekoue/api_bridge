package com.ialocalbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ialocalbridge.utils.NetworkHelper
import com.ialocalbridge.utils.WebInterface
import com.ialocalbridge.utils.FileUploader
import com.ialocalbridge.utils.FileMessageBuilder
import com.ialocalbridge.utils.ToolParser
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocalApiServer(private val port: Int, private val context: Context) : NanoHTTPD(null, port) {

    private val coordinator = AutomationCoordinator(context)
    private val fileUploader = FileUploader()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val TAG = "LocalApiServer"
    private val TIMEOUT_SECONDS = 180L
    private var totalPromptTokens = 0L
    private var totalCompletionTokens = 0L
    private var totalRequests = 0L
    private var streamEnabled = true

    init {
        val tempDir = File(context.cacheDir, "nanohttpd_temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        System.setProperty("java.io.tmpdir", tempDir.absolutePath)
        Log.d(TAG, "Server initialized at ${tempDir.absolutePath}")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (method == Method.OPTIONS) {
            val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
            addCORSHeaders(response)
            return response
        }

        if (uri == "/" || uri == "/index.html") {
            val ip = NetworkHelper.getIPAddress()
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, WebInterface.getHtml(ip, port, streamEnabled, totalRequests, totalPromptTokens, totalCompletionTokens))
        }

        return try {
            val response = when (uri) {
                "/v1/models" -> handleModelsRequest()
                "/v1/chat/completions" -> handleChatCompletions(session)
                "/status" -> handleStatusRequest()
                "/stats" -> handleStatsRequest()
                "/config" -> handleConfigRequest(session)
                "/ask" -> handleAskDirect(session)
                "/upload" -> handleUploadRequest(session)
                "/stop" -> {
                    mainHandler.post { runBlocking { coordinator.stopGeneration() } }
                    newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "STOP_SENT")
                }
                else -> jsonError(Response.Status.NOT_FOUND, "Not Found")
            }
            addCORSHeaders(response)
            response
        } catch (e: Exception) {
            Log.e(TAG, "Serve Exception", e)
            val response = jsonError(Response.Status.INTERNAL_ERROR, "Crash: ${e.message}")
            addCORSHeaders(response)
            response
        }
    }

    private fun handleStatusRequest(): Response {
        val isServiceActive = ClickAccessibilityService.instance != null
        val obj = JSONObject()
        obj.put("status", if (isServiceActive) "Ready" else "Accessibility Service Disabled")
        obj.put("stream_enabled", streamEnabled)
        obj.put("uptime_seconds", (System.currentTimeMillis() - startTime) / 1000)
        return newFixedLengthResponse(Response.Status.OK, "application/json", obj.toString())
    }

    private fun handleStatsRequest(): Response {
        val obj = JSONObject()
        obj.put("total_requests", totalRequests)
        obj.put("total_prompt_tokens", totalPromptTokens)
        obj.put("total_completion_tokens", totalCompletionTokens)
        obj.put("stream_enabled", streamEnabled)
        obj.put("uptime_seconds", (System.currentTimeMillis() - startTime) / 1000)
        return newFixedLengthResponse(Response.Status.OK, "application/json", obj.toString())
    }

    private fun handleConfigRequest(session: IHTTPSession): Response {
        if (session.method == Method.POST) {
            val body = parseBody(session)
            if (body.isNotEmpty()) {
                try {
                    val req = JSONObject(body)
                    if (req.has("stream_enabled")) {
                        streamEnabled = req.getBoolean("stream_enabled")
                    }
                } catch (_: Exception) {}
            }
        }
        val obj = JSONObject()
        obj.put("stream_enabled", streamEnabled)
        return newFixedLengthResponse(Response.Status.OK, "application/json", obj.toString())
    }

    private fun handleAskDirect(session: IHTTPSession): Response {
        val params = if (session.method == Method.POST) {
            val files = HashMap<String, String>()
            try { session.parseBody(files) } catch (_: Exception) {}
            session.parameters
        } else {
            session.parameters
        }
        val q = params["q"]?.get(0) ?: params["question"]?.get(0)
        if (q == null) return jsonError(Response.Status.BAD_REQUEST, "Missing 'q' parameter")
        val result = executeBlocking(q)
        return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", result)
    }

    private fun handleChatCompletions(session: IHTTPSession): Response {
        val body = parseBody(session)
        if (body.isEmpty()) return jsonError(Response.Status.BAD_REQUEST, "Missing request body")
        return try {
            val request = JSONObject(body)
            val messages = request.optJSONArray("messages")
            if (messages == null || messages.length() == 0) {
                return jsonError(Response.Status.BAD_REQUEST, "Missing messages array")
            }
            val model = request.optString("model", "deepseek-chat")
            val stream = request.optBoolean("stream", false)
            val tools = request.optJSONArray("tools")
            val lastMsg = messages.getJSONObject(messages.length() - 1)
            var prompt = lastMsg.optString("content", "")
            if (tools != null && tools.length() > 0) {
                val toolPrompt = ToolParser.buildToolCallPrompt(tools)
                prompt = prompt + "\n\n" + toolPrompt
            }
            val rawResult = executeBlocking(prompt)
            totalRequests++
            val promptTokens = maxOf(1, prompt.length / 4)
            val completionTokens = maxOf(1, rawResult.length / 4)
            totalPromptTokens += promptTokens
            totalCompletionTokens += completionTokens
            val parsed = ToolParser.parseToolCalls(rawResult)
            val effectiveStream = stream || streamEnabled
            if (effectiveStream) {
                return buildStreamResponse(model, rawResult, parsed, promptTokens, completionTokens)
            }
            return buildJsonResponse(model, rawResult, parsed, promptTokens, completionTokens)
        } catch (e: Exception) {
            Log.e(TAG, "Chat error", e)
            jsonError(Response.Status.BAD_REQUEST, "Invalid JSON: ${e.message}")
        }
    }

    private fun buildJsonResponse(model: String, rawResult: String, parsed: ToolParser.ToolCallResult?, promptTokens: Int, completionTokens: Int): Response {
        val respObj = JSONObject()
        respObj.put("id", "chatcmpl-" + UUID.randomUUID().toString().take(8))
        respObj.put("object", "chat.completion")
        respObj.put("created", System.currentTimeMillis() / 1000)
        respObj.put("model", model)
        val choices = JSONArray()
        val choice = JSONObject()
        choice.put("index", 0)
        val msg = JSONObject()
        msg.put("role", "assistant")
        if (parsed != null) {
            msg.put("content", parsed.remainingText.trim())
            val tcArr = JSONArray()
            for (tc in parsed.toolCalls) {
                val tco = JSONObject()
                tco.put("id", tc.id)
                tco.put("type", "function")
                val func = JSONObject()
                func.put("name", tc.name)
                func.put("arguments", tc.arguments)
                tco.put("function", func)
                tcArr.put(tco)
            }
            msg.put("tool_calls", tcArr)
            choice.put("finish_reason", "tool_calls")
        } else {
            msg.put("content", rawResult)
            choice.put("finish_reason", "stop")
        }
        choice.put("message", msg)
        choices.put(choice)
        respObj.put("choices", choices)
        val usage = JSONObject()
        usage.put("prompt_tokens", promptTokens)
        usage.put("completion_tokens", completionTokens)
        usage.put("total_tokens", promptTokens + completionTokens)
        respObj.put("usage", usage)
        return newFixedLengthResponse(Response.Status.OK, "application/json", respObj.toString(2))
    }

    private fun buildStreamResponse(model: String, rawResult: String, parsed: ToolParser.ToolCallResult?, promptTokens: Int, completionTokens: Int): Response {
        val chatId = "chatcmpl-" + UUID.randomUUID().toString().take(8)
        val content = if (parsed != null) parsed.remainingText.trim() else rawResult
        val finishReason = if (parsed != null) "tool_calls" else "stop"
        val pipedOut = PipedOutputStream()
        val pipedIn = PipedInputStream(pipedOut)
        Thread {
            try {
                val roleChunk = JSONObject()
                roleChunk.put("id", chatId)
                roleChunk.put("object", "chat.completion.chunk")
                roleChunk.put("created", System.currentTimeMillis() / 1000)
                roleChunk.put("model", model)
                val roleChoices = JSONArray()
                val roleChoice = JSONObject()
                roleChoice.put("index", 0)
                val roleDelta = JSONObject()
                roleDelta.put("role", "assistant")
                roleDelta.put("content", "")
                roleChoice.put("delta", roleDelta)
                roleChoice.put("finish_reason", JSONObject.NULL)
                roleChoices.put(roleChoice)
                roleChunk.put("choices", roleChoices)
                pipedOut.write("data: ${roleChunk.toString()}\n\n".toByteArray())
                pipedOut.flush()
                Thread.sleep(30)
                val chars = content.toCharArray()
                var i = 0
                while (i < chars.size) {
                    val chunkSize = minOf(15, chars.size - i)
                    val chunk = String(chars, i, chunkSize)
                    i += chunkSize
                    val chunkObj = JSONObject()
                    chunkObj.put("id", chatId)
                    chunkObj.put("object", "chat.completion.chunk")
                    chunkObj.put("created", System.currentTimeMillis() / 1000)
                    chunkObj.put("model", model)
                    val chunkChoices = JSONArray()
                    val chunkChoice = JSONObject()
                    chunkChoice.put("index", 0)
                    val delta = JSONObject()
                    delta.put("content", chunk)
                    chunkChoice.put("delta", delta)
                    chunkChoice.put("finish_reason", JSONObject.NULL)
                    chunkChoices.put(chunkChoice)
                    chunkObj.put("choices", chunkChoices)
                    pipedOut.write("data: ${chunkObj.toString()}\n\n".toByteArray())
                    pipedOut.flush()
                    Thread.sleep(40)
                }
                val finalChunk = JSONObject()
                finalChunk.put("id", chatId)
                finalChunk.put("object", "chat.completion.chunk")
                finalChunk.put("created", System.currentTimeMillis() / 1000)
                finalChunk.put("model", model)
                val finalChoices = JSONArray()
                val finalChoice = JSONObject()
                finalChoice.put("index", 0)
                val finalDelta = JSONObject()
                finalDelta.put("content", "")
                finalChoice.put("delta", finalDelta)
                finalChoice.put("finish_reason", finishReason)
                finalChoices.put(finalChoice)
                finalChunk.put("choices", finalChoices)
                val usage = JSONObject()
                usage.put("prompt_tokens", promptTokens)
                usage.put("completion_tokens", completionTokens)
                usage.put("total_tokens", promptTokens + completionTokens)
                finalChunk.put("usage", usage)
                pipedOut.write("data: ${finalChunk.toString()}\n\n".toByteArray())
                pipedOut.write("data: [DONE]\n\n".toByteArray())
                pipedOut.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Stream error", e)
            } finally {
                try { pipedOut.close() } catch (_: Exception) {}
            }
        }.start()
        return newChunkedResponse(Response.Status.OK, "text/event-stream", pipedIn)
    }

    private fun executeBlocking(prompt: String): String {
        val latch = CountDownLatch(1)
        val resultHolder = StringBuilder()
        mainHandler.post {
            runBlocking {
                try {
                    val result = coordinator.processQuestion(prompt)
                    resultHolder.append(result)
                } catch (e: Exception) {
                    resultHolder.append("ERROR: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        return resultHolder.toString().ifEmpty { "ERROR: Timeout after ${TIMEOUT_SECONDS}s" }
    }

    private fun handleModelsRequest(): Response {
        val model = JSONObject()
        model.put("id", "deepseek-chat")
        model.put("object", "model")
        model.put("created", 1699200000)
        model.put("owned_by", "nemapi-bridge")
        val models = JSONArray()
        models.put(model)
        val resp = JSONObject()
        resp.put("object", "list")
        resp.put("data", models)
        return newFixedLengthResponse(Response.Status.OK, "application/json", resp.toString(2))
    }

    private fun handleUploadRequest(session: IHTTPSession): Response {
        if (session.method != Method.POST) return jsonError(Response.Status.METHOD_NOT_ALLOWED, "POST required")
        val files = HashMap<String, String>()
        session.parseBody(files)
        val temp = files["file"]
        val name = session.parameters["file"]?.get(0) ?: "file.bin"
        return if (temp != null) {
            val res = fileUploader.uploadWithFallback(File(temp), name)
            if (res.success) {
                val json = "{\"success\":true,\"url\":\"${res.url}\"}"
                newFixedLengthResponse(Response.Status.OK, "application/json", json)
            } else {
                jsonError(Response.Status.INTERNAL_ERROR, res.errorLog)
            }
        } else jsonError(Response.Status.BAD_REQUEST, "No file")
    }

    private fun parseBody(session: IHTTPSession): String {
        if (session.method != Method.POST) return ""
        val files = HashMap<String, String>()
        return try {
            session.parseBody(files)
            files["postData"] ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun jsonError(status: Response.Status, message: String): Response {
        val err = JSONObject()
        err.put("error", message)
        return newFixedLengthResponse(status, "application/json", err.toString())
    }

    private fun addCORSHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
    }

    companion object {
        private val startTime = System.currentTimeMillis()
    }
}
