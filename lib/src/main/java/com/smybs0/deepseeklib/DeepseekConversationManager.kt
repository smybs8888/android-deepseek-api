package com.smybs0.deepseeklib

import com.google.gson.Gson
import com.smybs0.deepseeklib.entity.ConversationData
import com.smybs0.deepseeklib.entity.ConversationDescData
import com.smybs0.deepseeklib.room.ConversationDatabaseUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object DeepseekConversationManager {
    private val conversationDao by lazy { ConversationDatabaseUtils.getConversationDao() }

    private val descMap = ConcurrentHashMap<Long, ConversationDescData>()
    private val gson = Gson()


    // 向数据库插入一个新会话，并返回 元信息
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun insertEmptyConversation(userId: String): ConversationDescData {
        val curTime = System.currentTimeMillis()
        val conversationData = ConversationData(0, "", curTime, curTime, "[]", userId)
        val cid = conversationDao.insertNew(conversationData)

        GlobalScope.launch(Dispatchers.IO) {
            conversationDao.updateDescByCid(cid, "新对话$cid")
        }
        return conversationData.getConversationDescData()
            .apply { this.cid = cid; this.desc = "新对话$cid"; descMap.put(cid, this) }
    }

    // 创建一个空的会话
    suspend fun createConversation(userId: String): DeepseekConversation {
        return DeepseekConversation.create(insertEmptyConversation(userId))
    }

    // 创建一个空的会话 并 设置对方角色
    suspend fun createConversation(userId: String, characterSetting: String): DeepseekConversation {
        return DeepseekConversation.create(insertEmptyConversation(userId), characterSetting)
    }

    // 获取历史会话消息的 元信息列表(不包含内容)
    suspend fun getHistoryConversationDescList(userId: String): List<ConversationDescData> {
        return conversationDao.queryAllDescByUserId(userId).apply {
            forEach { data -> if (!descMap.contains(data.cid)) descMap.put(data.cid, data) }
        }
    }

    // 打开一个历史会话
    suspend fun openHistoryConversation(cid: Long): DeepseekConversation {
        val data: String? = conversationDao.queryDataByCid(cid)

        return if (data != null) {
            DeepseekConversation.open(descMap.get(cid)!!, data)
        } else {
            throw RuntimeException("请提供正确的cid(会话ID)")
        }
    }
}