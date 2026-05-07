package com.smybs0.deepseeklib.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smybs0.deepseeklib.entity.ConversationData

@Database(entities = [ConversationData::class], version = 1)
internal abstract class ConversationDatabase : RoomDatabase() {
    abstract fun getConversationDao(): ConversationDao
}

internal object ConversationDatabaseUtils {
    private lateinit var database: ConversationDatabase

    fun init(context: Context) {
        database = Room.databaseBuilder(
            context,
            ConversationDatabase::class.java,
            "deepseek_conversation_database"
        ).build()
    }

    fun getConversationDao(): ConversationDao {
        if (::database.isInitialized) {
            return database.getConversationDao()
        } else {
            throw IllegalStateException("使用前请调用DeepseekConfig.init()初始化")
        }
    }
}