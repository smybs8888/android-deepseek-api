# android-deepseek-api
Android，提供对deepseek api的封装。

DeepseekConfig.init("your deepseek api key") // 初始化

val conversation = DeepseekConversation.create() // 创建一个会话，自动管理历史消息
val conversation = DeepseekConversation.create(characterSetting = "你是一个资深android开发者") // 可选择指定对方角色(消息类型为system)

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
conversation.send(
    content,
    object : DeepseekConversation.Callback {
        override fun onSend(position: Int) {}
        override fun onSuccess(message: Message, position: Int) {}
        override fun onFailure(throwable: Throwable) {}
    },
    model, enabledThinking, reasoningEffort
)

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
conversation.sendStreaming(
    content,
    object : DeepseekConversation.StreamingCallback {
        override fun onSend(position: Int, assistantPosition: Int) {}
        override fun onUpdate(thinking: Boolean,contentIncrement: String,reasonContentIncrement: String) {}
        override fun onFinish(message: Message, positon: Int) {}
        override fun onFailure(throwable: Throwable) {}
    },
    model, enabledThinking, reasoningEffort
)
