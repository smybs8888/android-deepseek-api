package com.smybs0.deepseeklib.entity

import com.smybs0.deepseeklib.ChatRole

internal data class ChatResponse(
    val id: String,
    val choices: List<ResponseChoice>,
    val created: Int,
    val model: String,
    val system_fingerprint: String,
    val `object`: String,
    val usage: ResponseUsage,
)

internal data class ResponseChoice(
    val finish_reason: String,
    val index: Int,
    val message: ResponseMessage,
) {
    fun isNormalStop() = (finish_reason == "stop")
}

internal data class ResponseMessage(
    val role: String,
    val content: String,
    val reasoning_content: String,
) {
    val roleE: ChatRole get() = ChatRole.ASSISTANT
}

internal data class ResponseUsage(
    val completion_tokens: Int,
    val completion_tokens_details: CompletionTokensDetails,
    val prompt_cache_hit_tokens: Int,
    val prompt_cache_miss_tokens: Int,
    val prompt_tokens: Int,
    val prompt_tokens_details: PromptTokensDetails,
    val total_tokens: Int,
)

internal data class CompletionTokensDetails(val reasoning_tokens: Int)
internal data class PromptTokensDetails(val cached_tokens: Int)


internal data class ChatStreamResponse(
    val id: String,
    val choices: List<StreamResponseChoice>,
    val created: Int,
    val model: String,
    val system_fingerprint: String,
    val `object`: String,
)

internal data class StreamResponseChoice(
    val finish_reason: String,
    val index: Int,
    val delta: StreamResponseDelta,
) {
    fun isNormalStop() = (finish_reason == "stop")
}

internal data class StreamResponseDelta(
    val role: String,
    val content: String?,
    val reasoning_content: String?,
) {
    val roleE: ChatRole get() = ChatRole.ASSISTANT
}