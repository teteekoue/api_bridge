package com.ialocalbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ialocalbridge.utils.NetworkHelper
import com.ialocalbridge.utils.WebInterface
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.UUID

class LocalApiServer(private val port: Int, private val context: Context) : NanoHTTPD(port) {

}
