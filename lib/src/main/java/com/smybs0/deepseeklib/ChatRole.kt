package com.smybs0.deepseeklib

enum class ChatRole(val roleStr: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    companion object {
        fun pause(role: String): ChatRole {
            return when (role) {
                "system" -> SYSTEM
                "user" -> USER
                "assistant" -> ASSISTANT
                "tool" -> TOOL
                else -> throw RuntimeException("无法将${role}转为已有类型")
            }
        }
    }
}