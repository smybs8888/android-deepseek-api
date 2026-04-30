package com.smybs0.deepseeklib

import com.smybs0.deepseeklib.net.ChatApiService

object DeepseekConfig {
    private lateinit var mApiKey: String

    fun init(apiKey: String) {
        mApiKey = apiKey
        ChatApiService.init(apiKey)
    }
}