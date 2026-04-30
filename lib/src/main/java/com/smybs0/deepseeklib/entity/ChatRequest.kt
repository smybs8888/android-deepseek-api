package com.smybs0.deepseeklib.entity

import com.smybs0.deepseeklib.ChatRole

internal data class ChatRequest(
    val messages: List<RequestMessage>,
    val model: String,
    val thinking: RequestThinking,
    val reasoning_effort: String?,
    val stream: Boolean,
    val stream_options: RequestStreamOptions?,
)

internal data class RequestMessage(
    val role: String,
    val content: String,
    val name: String,
) {
    companion object {
        fun fromUser(content: String, name: String) =
            RequestMessage(ChatRole.USER.roleStr, content, name)

        fun fromSystem(content: String, name: String) =
            RequestMessage(ChatRole.SYSTEM.roleStr, content, name)
    }

    val roleE: ChatRole get() = ChatRole.pause(role)
}

internal data class RequestThinking private constructor(
    val type: String,
) {
    companion object {
        fun from(enabledThinking: Boolean): RequestThinking =
            if (enabledThinking) RequestThinking("enabled")
            else RequestThinking("disabled")
    }
}

internal data class RequestStreamOptions(
    val include_usage: Boolean,
)
