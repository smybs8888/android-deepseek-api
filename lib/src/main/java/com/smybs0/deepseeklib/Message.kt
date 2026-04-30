package com.smybs0.deepseeklib

data class Message(
    val role: ChatRole,
    val content: String,
    val reasonContent: String,
    val finishReason: String
)