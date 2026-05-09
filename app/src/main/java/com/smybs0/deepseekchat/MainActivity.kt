package com.smybs0.deepseekchat

import android.content.DialogInterface
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.smybs0.deepseekchat.databinding.ActivityMainBinding
import com.smybs0.deepseekchat.databinding.ItemMessageBinding
import com.smybs0.deepseeklib.DeepseekConversation
import com.smybs0.deepseeklib.DeepseekConversationManager
import com.smybs0.deepseeklib.DeepseekConversationManager.createConversation
import com.smybs0.deepseeklib.DeepseekMode
import com.smybs0.deepseeklib.entity.ConversationDescData
import com.smybs0.deepseeklib.entity.Message
import kotlinx.coroutines.launch

internal class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var conversation: DeepseekConversation

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var conversationAdapter: ConversationAdapter
    private val conversationList = ArrayList<ConversationDescData>()
    private var onDescChangeListener: DeepseekConversation.OnDescChangeListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.isConversationState = false
        binding.isEnabledThinking = true
        binding.rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        lifecycleScope.launch {
            conversationAdapter = ConversationAdapter(
                this@MainActivity,
                lifecycleScope,
                conversationList
            ) {
                binding.isConversationState = true
                setConversation(it)
            }
            binding.rvConversation.adapter = conversationAdapter
            conversationList.addAll(DeepseekConversationManager.getHistoryConversationDescList("0"))
            conversationAdapter.notifyDataSetChanged()
        }

        binding.btnNewConversation.setOnClickListener {
            val et = EditText(this)

            val clickListener = DialogInterface.OnClickListener { dialog, which ->
                if (which != AlertDialog.BUTTON_NEUTRAL) {
                    lifecycleScope.launch {
                        val setting = et.text.toString()
                        val conversation =
                            if (which == AlertDialog.BUTTON_POSITIVE && setting.isNotEmpty()) {
                                createConversation("0", setting)
                            } else {
                                createConversation("0")
                            }
                        conversationList.add(conversation.descData)
                        conversationAdapter.notifyItemInserted(conversationList.lastIndex)
                        setConversation(conversation)
                        conversationAdapter.setSelectedPosition(
                            conversationList.lastIndex,
                            conversation
                        )
                    }

                }
            }

            AlertDialog.Builder(this)
                .setTitle("指定对方角色")
                .setView(et)
                .setPositiveButton("创建", clickListener)
                .setNeutralButton("取消", clickListener)
                .setNegativeButton("不指定创建", clickListener)
                .show()
        }


        binding.btnSend.setOnClickListener {
            val content = binding.etContent.text.toString()
            binding.etContent.setText("")

            if (content.isNotEmpty()) {
                binding.btnSend.isEnabled = false
                val isStreaming = binding.cbStartStreaming.isChecked
                val enabledThinking = binding.isEnabledThinking!!
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

    private fun setConversation(conversation: DeepseekConversation) {
        this.conversation = conversation

        binding.tvDesc.text = conversation.desc
        onDescChangeListener = DeepseekConversation.OnDescChangeListener {
            binding.tvDesc.text = it
        }
        this.conversation.addOnDescChangeListener(onDescChangeListener!!)

        binding.isConversationState = true
        messageAdapter = MessageAdapter(this, conversation.messageList)
        binding.rv.adapter = messageAdapter
    }
}