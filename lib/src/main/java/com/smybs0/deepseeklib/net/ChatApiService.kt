package com.smybs0.deepseeklib.net

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.smybs0.deepseeklib.ChatRole
import com.smybs0.deepseeklib.entity.ChatRequest
import com.smybs0.deepseeklib.entity.ChatResponse
import com.smybs0.deepseeklib.entity.ChatStreamResponse
import com.smybs0.deepseeklib.entity.RequestMessage
import com.smybs0.deepseeklib.entity.RequestStreamOptions
import com.smybs0.deepseeklib.entity.RequestThinking
import com.smybs0.deepseeklib.entity.ResponseChoice
import com.smybs0.deepseeklib.entity.ResponseMessage
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object ChatApiService {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .addInterceptor {
            if (apiKey == null) {
                throw IllegalStateException("使用前请调用DeepseekConfig.init()提供正确的 api key")
            }
            it.proceed(it.request().newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .build())
        }
        .build()
    private val requestMediaType = MediaType.parse("application/json")
    private var apiKey: String? = null
    val handler = Handler(Looper.getMainLooper())

    fun askDeepseek(
        historyMessages: List<RequestMessage>,
        model: String,
        enabledThinking: Boolean,
        reasoningEffort: String?,
        callback: Callback,
    ) {
        try {
            val chatRequest = ChatRequest(
                historyMessages,
                model,
                RequestThinking.Companion.from(enabledThinking),
                if (enabledThinking) reasoningEffort else null,
                false, null
            )

            val request = Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .post(RequestBody.create(requestMediaType, gson.toJson(chatRequest)))
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call?, e: IOException) {
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call?, response: Response) {
                    if (!response.isSuccessful) {
                        callback.onFailure(RuntimeException("HTTP ${response.code()} : ${response.body()?.string()}"))
                        return
                    }

                    val result = gson.fromJson(response.body()?.string(), ChatResponse::class.java)

                    val choice = result.choices[0]
                    if (choice.isNormalStop()) {
                        callback.onSuccess(choice)
                    } else {
                        callback.onFailure(Throwable("ask Deepseek Failed ${choice.finish_reason}"))
                    }
                }
            })

        } catch (e: Exception) {
            callback.onFailure(e)
        }
    }

    fun askDeepseekStreaming(
        historyMessages: List<RequestMessage>,
        model: String,
        enabledThinking: Boolean,
        reasoningEffort: String?,
        callback: StreamingCallback,
    ) {
        try {
            val chatRequest = ChatRequest(
                historyMessages,
                model,
                RequestThinking.Companion.from(enabledThinking),
                if (enabledThinking) reasoningEffort else null,
                true, RequestStreamOptions(true)
            )

            val request = Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .post(RequestBody.create(requestMediaType, gson.toJson(chatRequest)))
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call?, e: IOException) {
                    callback.onFailure(e)
                }

                override fun onResponse(call: Call?, response: Response) {
                    if (!response.isSuccessful) {
                        callback.onFailure(RuntimeException("HTTP ${response.code()} : ${response.body()?.string()}"))
                        return
                    }

                    val bufferedReader = response.body()!!.byteStream().bufferedReader()
                    var lineStr: String?
                    val contentBuilder = StringBuilder()
                    val reasonContentBuilder = StringBuilder()
                    var finishReason: String? = null
                    var index = 0

                    while (bufferedReader.readLine().apply { lineStr = this } != null) {
                        if (lineStr!!.startsWith("data: ")) {
                            lineStr = lineStr.substring(6)

                            if (lineStr == "[DONE]") {
                                break
                            }

                            val responsePart = gson.fromJson(lineStr, ChatStreamResponse::class.java)
                            val choice = responsePart.choices[0]
                            val contentIncrement = choice.delta.content.orEmpty()
                            val reasonContentIncrement = choice.delta.reasoning_content.orEmpty()

                            callback.onUpdate(
                                reasonContentIncrement.isNotEmpty(),
                                contentIncrement,
                                reasonContentIncrement
                            )
                            finishReason = choice.finish_reason
                            contentBuilder.append(contentIncrement)
                            reasonContentBuilder.append(reasonContentIncrement)
                        }
                    }

                    callback.onFinish(
                        ResponseChoice(
                            finishReason ?: "unknown cause",
                            index,
                            ResponseMessage(
                                ChatRole.ASSISTANT.roleStr,
                                contentBuilder.toString(),
                                reasonContentBuilder.toString()
                            )
                        )
                    )
                }

            })

        } catch (e: Exception) {
            callback.onFailure(e)
        }
    }

    interface Callback {
        fun onSuccess(choice: ResponseChoice)
        fun onFailure(throwable: Throwable)
    }

    interface StreamingCallback {
        fun onUpdate(thinking: Boolean, contentIncrement: String, reasonContentIncrement: String)
        fun onFinish(choice: ResponseChoice)
        fun onFailure(throwable: Throwable)
    }

    fun init(apiKey: String) {
        this.apiKey = apiKey
    }
}