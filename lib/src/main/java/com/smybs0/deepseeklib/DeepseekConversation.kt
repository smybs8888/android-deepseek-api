package com.smybs0.deepseeklib

import android.telecom.CallAudioState
import com.google.gson.Gson
import com.smybs0.deepseeklib.DeepseekConversation.Companion.gson
import com.smybs0.deepseeklib.DeepseekMode.MODEL_V4_FLASH
import com.smybs0.deepseeklib.DeepseekMode.REASONING_EFFORT_HIGH
import com.smybs0.deepseeklib.entity.ChatRole
import com.smybs0.deepseeklib.entity.ConversationDescData
import com.smybs0.deepseeklib.entity.Message
import com.smybs0.deepseeklib.entity.RequestMessage
import com.smybs0.deepseeklib.entity.ResponseChoice
import com.smybs0.deepseeklib.net.ChatApiService
import com.smybs0.deepseeklib.room.ConversationDatabaseUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class DeepseekConversation private constructor() {
    companion object {
        private val conversationDao by lazy { ConversationDatabaseUtils.getConversationDao() }
        private val gson = Gson()

        // 创建一个新的空会话
        internal fun create(conversationDescData: ConversationDescData): DeepseekConversation {
            val conversation = DeepseekConversation()
            conversation.mConversationDescData = conversationDescData
            return conversation
        }

        // 创建一个会话，并设置deepseek扮演的角色
        internal fun create(
            conversationDescData: ConversationDescData,
            characterSetting: String,
        ): DeepseekConversation {
            val conversation = DeepseekConversation()
            conversation.mConversationDescData = conversationDescData
            conversation.mMessageList.add(Message(ChatRole.SYSTEM, characterSetting, "", ""))
            conversation.updateData()
            return conversation
        }

        // 恢复历史会话
        internal fun open(
            conversationDescData: ConversationDescData,
            historyMessagesData: String,
        ): DeepseekConversation {
            val conversation = DeepseekConversation()
            conversation.mConversationDescData = conversationDescData
            val historyMessageList =
                gson.fromJson(historyMessagesData, Array<Message>::class.java).toList()
            conversation.mMessageList.addAll(historyMessageList)
            return conversation
        }

        /*
        总结提示消息
     */
        val summarizeMessage = Message(
            ChatRole.USER,
            "对当前对话内容进行总结，不超过10字符，不带除空格外任何标点符号",
            "",
            ""
        )
    }


    private lateinit var mConversationDescData: ConversationDescData
    private val mMessageList = ArrayList<Message>()
    private val mIsReasoning = AtomicBoolean(false)

    /*
        消息列表(包括 系统/用户/回复)
     */
    val messageList: List<Message> get() = mMessageList

    /*
        是否正在思考(即发送消息后还没有收到完整回复)
        如果正在思考就不能发送消息
     */
    val isReasoning: Boolean get() = mIsReasoning.get()

    /*
        获取会话id / 描述信息 / 创建时间 / 最后一次消息时间 / 用户id
     */
    val conversationId: Long get() = mConversationDescData.cid
    val desc: String get() = mConversationDescData.desc
    val createTime: Long get() = mConversationDescData.createTime
    val lastTime: Long get() = mConversationDescData.lastTime
    val userId: String get() = mConversationDescData.userId

    /*
        获取元信息
     */
    val descData: ConversationDescData get() = mConversationDescData

    /*
        存放描述信息变化的监听器的列表
     */
    private val onDescChangeListenerList = ArrayList<WeakReference<OnDescChangeListener>>()


    /*
        私有方法，保存当前对话记录到数据库
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun updateData() {
        val curTime = System.currentTimeMillis()
        val messageList = ArrayList(mMessageList)
        val data = gson.toJson(messageList)

        GlobalScope.launch(Dispatchers.IO) {
            conversationDao.updateDataByCid(mConversationDescData.cid, data, curTime)
        }
    }


    /*
        私有方法，对当前对话进行总结
     */
    private fun updateDesc() {
        if (mMessageList.size < 10 || mConversationDescData.desc.startsWith("新对话")) {
            DeepseekAsk.summarize(
                ArrayList(mMessageList + summarizeMessage),
                object : DeepseekAsk.Callback {
                    override fun onSuccess(message: Message) {
                        val newDesc = message.content
                        mConversationDescData.desc = newDesc
                        ChatApiService.handler.post {
                            onDescChangeListenerList.forEach { it.get()?.onDescChange(newDesc) }
                        }
                        conversationDao.updateDescByCid(mConversationDescData.cid, newDesc)
                    }

                    override fun onFailure(throwable: Throwable) {}
                },
                enabledThinking = false
            )
        }
    }


    /*
        content: 发送的消息内容

        callback: 回调
            // 当发送消息时，会将用户消息插入消息列表，并返回插入的位置
            onSend(position: Int)
            // 当成功收到回复时，会将收到的消息插入消息列表，并返回内容和插入的位置
            onSuccess(message: Message, position: Int)
            // 当各种原因导致失败时，返回异常
            onFailure(throwable: Throwable)

        model: 调用模型名称
        enabledThinking: 是否开启深度思考
        reasoningEffort: 思考强度(high/max) 开启深度思考模式可选
     */
    fun send(
        content: String,
        callback: Callback,
        model: String = MODEL_V4_FLASH,
        enabledThinking: Boolean = true,
        reasoningEffort: String? = REASONING_EFFORT_HIGH,
    ) {
        if (mIsReasoning.getAndSet(true)) {
            callback.onFailure(RuntimeException("正在推理中，无法发送消息"))
            return
        }

        mMessageList.add(Message(ChatRole.USER, content, "", ""))
        ChatApiService.handler.post { callback.onSend(mMessageList.lastIndex) }
        updateData()

        ChatApiService.askDeepseek(
            mMessageList
                .map { msg -> RequestMessage(msg.role.roleStr, msg.content, "") },
            model,
            enabledThinking,
            reasoningEffort,
            object : ChatApiService.Callback {
                override fun onSuccess(choice: ResponseChoice) {
                    val message = Message(
                        choice.message.roleE,
                        choice.message.content.orEmpty(),
                        choice.message.reasoning_content.orEmpty(),
                        choice.finish_reason
                    )
                    mMessageList.add(message)
                    ChatApiService.handler.post {
                        callback.onSuccess(message, mMessageList.lastIndex)
                    }
                    updateData()
                    updateDesc()
                    mIsReasoning.set(false)
                }

                override fun onFailure(throwable: Throwable) {
                    ChatApiService.handler.post {
                        callback.onFailure(throwable)
                    }
                    mIsReasoning.set(false)
                }
            }
        )
    }

    /*
        content: 发送的消息内容

        callback: 回调
            // 当发送消息时，会将用户消息插入消息列表，
               并插入一条占位消息(这条消息不会因为流式响应更新，直到响应结束被替换为完整内容)，
               返回用户消息位置和占位消息位置
            onSend(position: Int, assistantPosition: Int)
            // 当收到流式响应时，返回增量内容
            onUpdate(thinking: Boolean, contentIncrement: String, reasonContentIncrement: String)
            // 当成功收到回复时，会将收到的消息插入消息列表，并返回内容和插入的位置
            onFinish(message: Message, position: Int)
            // 当各种原因导致失败时，返回异常
            onFailure(throwable: Throwable)

        model: 调用模型名称
        enabledThinking: 是否开启深度思考
        reasoningEffort: 思考强度(high/max) 开启深度思考模式可选
     */
    fun sendStreaming(
        content: String,
        callback: StreamingCallback,
        model: String = MODEL_V4_FLASH,
        enabledThinking: Boolean = true,
        reasoningEffort: String? = REASONING_EFFORT_HIGH,
    ) {
        if (mIsReasoning.getAndSet(true)) {
            callback.onFailure(RuntimeException("正在推理中，无法发送消息"))
            return
        }

        mMessageList.add(Message(ChatRole.USER, content, "", ""))
        val position = mMessageList.lastIndex
        updateData()
        mMessageList.add(Message(ChatRole.ASSISTANT, "", "", ""))
        val assistantPosition = mMessageList.lastIndex
        ChatApiService.handler.post { callback.onSend(position, assistantPosition) }

        ChatApiService.askDeepseekStreaming(
            mMessageList.dropLast(1)
                .map { msg -> RequestMessage(msg.role.roleStr, msg.content, "") },
            model,
            enabledThinking,
            reasoningEffort,
            object : ChatApiService.StreamingCallback {
                override fun onUpdate(
                    thinking: Boolean,
                    contentIncrement: String,
                    reasonContentIncrement: String,
                ) {
                    ChatApiService.handler.post {
                        callback.onUpdate(thinking, contentIncrement, reasonContentIncrement)
                    }
                }

                override fun onFinish(choice: ResponseChoice) {
                    val message = Message(
                        choice.message.roleE,
                        choice.message.content.orEmpty(),
                        choice.message.reasoning_content.orEmpty(),
                        choice.finish_reason
                    )
                    mMessageList[assistantPosition] = message
                    ChatApiService.handler.post {
                        callback.onFinish(message, assistantPosition)
                    }
                    updateData()
                    updateDesc()
                    mIsReasoning.set(false)
                }

                override fun onFailure(throwable: Throwable) {
                    if (mMessageList[assistantPosition].role == ChatRole.ASSISTANT) {
                        mMessageList.removeAt(assistantPosition)
                    }
                    ChatApiService.handler.post {
                        callback.onFailure(throwable)
                    }
                    mIsReasoning.set(false)
                }
            }
        )
    }

    /*
        在当前对话的描述信息发生变化时，返回新的描述信息
     */
    fun addOnDescChangeListener(onDescChangeListener: OnDescChangeListener) {
        onDescChangeListenerList.add(WeakReference(onDescChangeListener))
    }

    /*
        移除描述信息发生变化监听器
     */
    fun removeOnDescChangeListener() {
        onDescChangeListenerList.clear()
    }


    /*
            // 在当前对话的描述信息发生变化时，返回新的描述信息
     */
    fun interface OnDescChangeListener {
        fun onDescChange(newDesc: String)
    }

    /*
    回调
            // 当发送消息时，会将用户消息插入消息列表，并返回插入的位置
            onSend(position: Int)
            // 当成功收到回复时，会将收到的消息插入消息列表，并返回内容和插入的位置
            onSuccess(message: Message, position: Int)
            // 当各种原因导致失败时，返回异常
            onFailure(throwable: Throwable)
     */
    interface Callback {
        fun onSend(position: Int)
        fun onSuccess(message: Message, position: Int)
        fun onFailure(throwable: Throwable)
    }

    /*
    回调
            // 当发送消息时，会将用户消息插入消息列表，
               并插入一条占位消息(这条消息不会因为流式响应更新，直到响应结束被替换为完整内容)，
               返回用户消息位置和占位消息位置
            onSend(position: Int, assistantPosition: Int)
            // 当收到流式响应时，返回增量内容
            onUpdate(thinking: Boolean, contentIncrement: String, reasonContentIncrement: String)
            // 当成功收到回复时，会将收到的消息插入消息列表，并返回内容和插入的位置
            onFinish(message: Message, position: Int)
            // 当各种原因导致失败时，返回异常
            onFailure(throwable: Throwable)
     */
    interface StreamingCallback {
        fun onSend(position: Int, assistantPosition: Int)
        fun onUpdate(thinking: Boolean, contentIncrement: String, reasonContentIncrement: String)
        fun onFinish(message: Message, positon: Int)
        fun onFailure(throwable: Throwable)
    }
}