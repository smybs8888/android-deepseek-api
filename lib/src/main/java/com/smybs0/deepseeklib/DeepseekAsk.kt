package com.smybs0.deepseeklib

import com.smybs0.deepseeklib.DeepseekMode.MODEL_V4_FLASH
import com.smybs0.deepseeklib.DeepseekMode.REASONING_EFFORT_HIGH
import com.smybs0.deepseeklib.entity.RequestMessage
import com.smybs0.deepseeklib.entity.ResponseChoice
import com.smybs0.deepseeklib.net.ChatApiService

/*
    单次访问，不携带上下文和历史消息
 */
object DeepseekAsk {
    fun send(
        content: String,
        callback: Callback,
        model: String = MODEL_V4_FLASH,
        enabledThinking: Boolean = true,
        reasoningEffort: String? = REASONING_EFFORT_HIGH,
    ) = ChatApiService.askDeepseek(
        listOf(RequestMessage(ChatRole.USER.roleStr, content, "")),
        model,
        enabledThinking,
        reasoningEffort,
        object : ChatApiService.Callback {
            override fun onSuccess(choice: ResponseChoice) {
                val message = Message(
                    choice.message.roleE,
                    choice.message.content,
                    choice.message.reasoning_content,
                    choice.finish_reason
                )
                callback.onSuccess(message)
            }

            override fun onFailure(throwable: Throwable) {
                callback.onFailure(throwable)
            }
        }
    )

    fun sendStreaming(
        content: String,
        callback: StreamingCallback,
        model: String = MODEL_V4_FLASH,
        enabledThinking: Boolean = true,
        reasoningEffort: String? = REASONING_EFFORT_HIGH,
    ) = ChatApiService.askDeepseekStreaming(
        listOf(RequestMessage(ChatRole.USER.roleStr, content, "")),
        model,
        enabledThinking,
        reasoningEffort,
        object : ChatApiService.StreamingCallback {
            override fun onUpdate(
                thinking: Boolean,
                contentIncrement: String,
                reasonContentIncrement: String,
            ) {
                callback.onUpdate(thinking, contentIncrement, reasonContentIncrement)
            }

            override fun onFinish(choice: ResponseChoice) {
                val message = Message(
                    choice.message.roleE,
                    choice.message.content,
                    choice.message.reasoning_content,
                    choice.finish_reason
                )
                callback.onFinish(message)
            }

            override fun onFailure(throwable: Throwable) {
                callback.onFailure(throwable)
            }
        }
    )

    interface Callback {
        fun onSuccess(message: Message)
        fun onFailure(throwable: Throwable)
    }

    interface StreamingCallback {
        fun onUpdate(thinking: Boolean, contentIncrement: String, reasonContentIncrement: String)
        fun onFinish(message: Message)
        fun onFailure(throwable: Throwable)
    }
}