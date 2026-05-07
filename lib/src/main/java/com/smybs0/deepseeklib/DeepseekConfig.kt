package com.smybs0.deepseeklib

import android.content.Context
import com.smybs0.deepseeklib.net.ChatApiService
import com.smybs0.deepseeklib.room.ConversationDatabaseUtils

object DeepseekConfig {
    private lateinit var mApiKey: String

    fun init(context: Context, apiKey: String) {
        mApiKey = apiKey
        ChatApiService.init(apiKey)
        ConversationDatabaseUtils.init(context.applicationContext)
    }
}