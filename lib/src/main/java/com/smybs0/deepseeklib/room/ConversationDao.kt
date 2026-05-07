package com.smybs0.deepseeklib.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.smybs0.deepseeklib.entity.ConversationData
import com.smybs0.deepseeklib.entity.ConversationDescData

@Dao
internal interface ConversationDao {
    @Insert
    suspend fun insertNew(conversationData: ConversationData): Long

    @Query("delete from conversation_data_table where cid = :cid")
    suspend fun deleteByCid(cid: Long): Int

    @Query("select cid,`desc`,createTime,lastTime,userId from conversation_data_table where userId = :userId")
    suspend fun queryAllDescByUserId(userId: String): List<ConversationDescData>

    @Query("select data from conversation_data_table where cid = :cid limit 1")
    suspend fun queryDataByCid(cid: Long): String?

    @Query("update conversation_data_table set data = :newData, lastTime = :lastTime where cid = :cid")
    fun updateDataByCid(cid: Long, newData: String, lastTime: Long)

    @Query("update conversation_data_table set `desc` = :newDesc where cid = :cid")
    fun updateDescByCid(cid: Long, newDesc: String)
}