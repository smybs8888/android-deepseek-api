# android-deepseek-api
Android，提供对deepseek api的封装。

初始化
在应用启动时进行初始化，只需一次：

kotlin
DeepseekConfig.init("your deepseek api key")
创建会话
kotlin
// 默认会话
val conversation = DeepseekConversation.create()

// 自定义角色设定（system 消息）
val conversation = DeepseekConversation.create(
    characterSetting = "你是一个资深 Android 开发者"
)
会话会自动管理历史消息。

发送消息（非流式）
kotlin
conversation.send(
    content = "你的问题",
    callback = object : DeepseekConversation.Callback {
        override fun onSend(position: Int) {
            // 用户消息已插入历史列表，position 为其位置
        }

        override fun onSuccess(message: Message, position: Int) {
            // 收到完整回复，已插入历史列表
        }

        override fun onFailure(throwable: Throwable) {
            // 请求失败
        }
    },
    model = "deepseek-chat",           // 可选，默认模型
    enabledThinking = true,             // 是否开启深度思考
    reasoningEffort = "high"            // 可选：high / max
)
发送消息（流式）
流式模式下，会先插入一条占位消息用于实时更新。

kotlin
conversation.sendStreaming(
    content = "你的问题",
    callback = object : DeepseekConversation.StreamingCallback {
        override fun onSend(position: Int, assistantPosition: Int) {
            // 用户消息位置 / 助手占位消息位置
        }

        override fun onUpdate(
            thinking: Boolean,
            contentIncrement: String,
            reasonContentIncrement: String
        ) {
            // 实时增量内容
            // thinking: 当前是否为思考阶段
        }

        override fun onFinish(message: Message, position: Int) {
            // 流式响应结束，占位消息被替换为完整 Message
        }

        override fun onFailure(throwable: Throwable) {
            // 请求失败
        }
    },
    model = "deepseek-chat",
    enabledThinking = true,
    reasoningEffort = "max"
)
参数说明
参数	说明
content	发送的消息内容
model	调用的模型名称（如 deepseek-chat）
enabledThinking	是否开启深度思考模式
reasoningEffort	思考强度，仅在 enabledThinking = true 时有效，可选 high / max
回调说明
Callback（非流式）
方法	说明
onSend(position)	用户消息已插入历史列表
onSuccess(message, position)	助手回复已插入历史列表
onFailure(throwable)	请求失败
StreamingCallback（流式）
方法	说明
onSend(position, assistantPosition)	用户消息和占位消息已插入
onUpdate(thinking, contentIncrement, reasonContentIncrement)	收到增量数据（实时更新 UI）
onFinish(message, position)	流式响应完成，占位消息被替换
onFailure(throwable)	请求失败
注意事项
请确保在发起请求前已完成 DeepseekConfig.init()

流式模式下，onUpdate 可能被多次调用

reasoningEffort 仅当 enabledThinking = true 时生效

会话对象会自动管理上下文，无需手动维护消息列表

如果有需要补充或调整的地方，欢迎随时提出。
