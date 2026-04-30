package com.smybs0.deepseekchat

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smybs0.deepseekchat.databinding.ActivityMainBinding
import com.smybs0.deepseekchat.databinding.ItemMessageBinding
import com.smybs0.deepseeklib.DeepseekConfig
import com.smybs0.deepseeklib.DeepseekConversation
import com.smybs0.deepseeklib.DeepseekMode
import com.smybs0.deepseeklib.Message

internal class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val conversation = DeepseekConversation.create("你是一个资深android开发者")

    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        DeepseekConfig.init("your deepseek api key")

        messageAdapter = MessageAdapter(this, conversation.messageList)
        binding.rv.adapter = messageAdapter

        binding.cbEnableThinking.setOnCheckedChangeListener { _, isChecked ->
            binding.rgReasoningEffort.visibility = if (isChecked) View.VISIBLE else View.INVISIBLE
        }


        binding.btnSend.setOnClickListener {
            val content = binding.etContent.text.toString()

            if (content.isNotEmpty()) {
                binding.btnSend.isEnabled = false
                val isStreaming = binding.cbStartStreaming.isChecked
                val enabledThinking = binding.cbEnableThinking.isChecked
                val model = if (binding.rgModel.checkedRadioButtonId == R.id.rb_model_flash) {
                    DeepseekMode.MODEL_V4_FLASH
                } else {
                    DeepseekMode.MODEL_V4_PRO
                }
                val reasoningEffort =
                    if (binding.rgReasoningEffort.checkedRadioButtonId == R.id.rb_effort_high) {
                        DeepseekMode.REASONING_EFFORT_HIGH
                    } else {
                        DeepseekMode.REASONING_EFFORT_MAX
                    }

                if (isStreaming) {
                    conversation.sendStreaming(
                        content,
                        object : DeepseekConversation.StreamingCallback {
                            var assistantPosition = -1
                            var itemBinding: ItemMessageBinding? = null
                            var cacheContent = ""
                            var cacheReasonContent = ""

                            override fun onSend(position: Int, assistantPosition: Int) {
                                this.assistantPosition = assistantPosition
                                messageAdapter.notifyItemInserted(position)
                                messageAdapter.notifyItemInserted(assistantPosition)
                            }

                            override fun onUpdate(
                                thinking: Boolean,
                                contentIncrement: String,
                                reasonContentIncrement: String,
                            ) {
                                if (itemBinding == null) {
                                    itemBinding = messageAdapter.getBinding(assistantPosition)
                                    cacheContent += contentIncrement
                                    cacheReasonContent += reasonContentIncrement
                                    if (itemBinding != null) {
                                        itemBinding!!.content = cacheContent
                                        itemBinding!!.reasonContent = cacheReasonContent
                                    }
                                } else {
                                    if (contentIncrement.isNotEmpty()) {
                                        itemBinding!!.content += contentIncrement
                                    }
                                    if (reasonContentIncrement.isNotEmpty()) {
                                        itemBinding!!.reasonContent += reasonContentIncrement
                                    }
                                }
                            }

                            override fun onFinish(message: Message, positon: Int) {
                                messageAdapter.notifyItemChanged(positon)
                                Toast.makeText(
                                    this@MainActivity,
                                    "接收完成",
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.btnSend.isEnabled = true
                            }

                            override fun onFailure(throwable: Throwable) {
                                Toast.makeText(
                                    this@MainActivity,
                                    throwable.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                throwable.printStackTrace()
                                binding.btnSend.isEnabled = true
                            }
                        },
                        model, enabledThinking, reasoningEffort
                    )
                } else {
                    conversation.send(
                        content,
                        object : DeepseekConversation.Callback {
                            override fun onSend(position: Int) {
                                messageAdapter.notifyItemInserted(position)
                            }

                            override fun onSuccess(message: Message, position: Int) {
                                messageAdapter.notifyItemInserted(position)
                                binding.btnSend.isEnabled = true
                            }

                            override fun onFailure(throwable: Throwable) {
                                Toast.makeText(
                                    this@MainActivity,
                                    throwable.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                throwable.printStackTrace()
                                binding.btnSend.isEnabled = true
                            }

                        },
                        model, enabledThinking, reasoningEffort
                    )
                }

            } else {
                Toast.makeText(this, "消息不能为空", Toast.LENGTH_SHORT).show()
            }
        }
    }
}