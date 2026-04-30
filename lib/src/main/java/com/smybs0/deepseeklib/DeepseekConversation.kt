package com.smybs0.deepseeklib

import com.smybs0.deepseeklib.DeepseekMode.MODEL_V4_FLASH
import com.smybs0.deepseeklib.DeepseekMode.REASONING_EFFORT_HIGH
import com.smybs0.deepseeklib.entity.RequestMessage
import com.smybs0.deepseeklib.entity.ResponseChoice
import com.smybs0.deepseeklib.net.ChatApiService
import java.util.concurrent.atomic.AtomicBoolean

class DeepseekConversation private constructor() {
    companion object {
        // 创建一个新的空会话
        fun create() = DeepseekConversation()

        // 创建一个会话，并设置deepseek扮演的角色
        fun create(characterSetting: String): DeepseekConversation {
            val conversation = DeepseekConversation()
            conversation.mMessageList.add(Message(ChatRole.SYSTEM, characterSetting, "", ""))
            return conversation
        }
    }

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
                        choice.message.content,
                        choice.message.reasoning_content,
                        choice.finish_reason
                    )
                    mMessageList.add(message)
                    ChatApiService.handler.post {
                        callback.onSuccess(message, mMessageList.lastIndex)
                    }
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
                        choice.message.content,
                        choice.message.reasoning_content,
                        choice.finish_reason
                    )
                    mMessageList[assistantPosition] = message
                    ChatApiService.handler.post {
                        callback.onFinish(message, assistantPosition)
                    }
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