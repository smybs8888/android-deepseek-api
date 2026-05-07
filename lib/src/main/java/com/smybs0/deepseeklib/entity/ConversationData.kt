package com.smybs0.deepseeklib.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("conversation_data_table")
data class ConversationData(
    @PrimaryKey(autoGenerate = true)
    val cid: Long,
    var desc: String,
    val createTime: Long,
    var lastTime: Long,
    var data: String,
    val userId: String,
) {
    fun getConversationDescData() = ConversationDescData(cid, desc, createTime, lastTime, userId)
}

data class ConversationDescData(
    var cid: Long,
    var desc: String,
    val createTime: Long,
    var lastTime: Long,
    val userId: String,
)
