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
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocalApiServer(private val port: Int, private val context: Context) : NanoHTTPD(null, port) {

    private val coordinator = AutomationCoordinator(context)
    private val fileUploader = FileUploader()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val TAG = "LocalApiServer"
    private val TIMEOUT_SECONDS = 180L

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
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, WebInterface.getHtml(ip, port))
        }

        return try {
            val response = when (uri) {
                "/v1/models" -> handleModelsRequest()
                "/v1/chat/completions" -> handleChatCompletionsDirect(session)
                "/status" -> {
                    val isServiceActive = ClickAccessibilityService.instance != null
                    val status = if (isServiceActive) "Ready" else "Accessibility Service Disabled"
                    newFixedLengthResponse(status)
                }
                "/ask" -> handleAskDirect(session)
                "/upload" -> handleUploadRequest(session)
                "/stop" -> {
                    mainHandler.post {
                        runBlocking { coordinator.stopGeneration() }
                    }
                    newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "STOP_SENT")
                }
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Not Found\"}")
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

    private fun handleChatCompletionsDirect(session: IHTTPSession): Response {
        val body = parseBody(session)
        if (body.isEmpty()) return jsonError(Response.Status.BAD_REQUEST, "Missing request body")
        return try {
            val request = JSONObject(body)
            val messages = request.optJSONArray("messages")
            if (messages == null || messages.length() == 0) {
                return jsonError(Response.Status.BAD_REQUEST, "Missing messages array")
            }
            val model = request.optString("model", "deepseek-chat")
            val tools = request.optJSONArray("tools")
            val lastMsg = messages.getJSONObject(messages.length() - 1)
            var prompt = lastMsg.optString("content", "")
            if (tools != null && tools.length() > 0) {
                val toolPrompt = ToolParser.buildToolCallPrompt(tools)
                prompt = prompt + "\n\n" + toolPrompt
            }
            val rawResult = executeBlocking(prompt)
            val parsed = ToolParser.parseToolCalls(rawResult)
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
            usage.put("prompt_tokens", 0)
            usage.put("completion_tokens", 0)
            usage.put("total_tokens", 0)
            respObj.put("usage", usage)
            newFixedLengthResponse(Response.Status.OK, "application/json", respObj.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Chat error", e)
            jsonError(Response.Status.BAD_REQUEST, "Invalid JSON: ${e.message}")
        }
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
}
