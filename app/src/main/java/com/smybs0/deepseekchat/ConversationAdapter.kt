package com.smybs0.deepseekchat

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.smybs0.deepseekchat.databinding.ItemConversationBinding
import com.smybs0.deepseeklib.DeepseekConversation
import com.smybs0.deepseeklib.DeepseekConversationManager
import com.smybs0.deepseeklib.entity.ConversationDescData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ConversationAdapter(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val list: List<ConversationDescData>,
    private val onClickItemListener: (DeepseekConversation) -> Unit,
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {
    private var curSelectPosition = -1
    private lateinit var selectedConversation: DeepseekConversation
    private var onDescChangeListener: DeepseekConversation.OnDescChangeListener? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemConversationBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.run {
            desc = item.desc
            time = handleTimestamp(item.createTime)

            if (curSelectPosition == position) {
                root.setBackgroundColor("#FFEEEEEE".toColorInt())
                onDescChangeListener =
                    DeepseekConversation.OnDescChangeListener { holder.binding.desc = it }
                selectedConversation.addOnDescChangeListener(onDescChangeListener!!)
            } else {
                root.setBackgroundColor(Color.WHITE)
            }

            root.setOnClickListener {
                lifecycleScope.launch {
                    val openHistoryConversation =
                        DeepseekConversationManager.openHistoryConversation(item.cid)

                    setSelectedPosition(position, openHistoryConversation)
                    onClickItemListener(openHistoryConversation)
                }
            }
        }
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int): Long {
        return list[position].cid
    }


    fun setSelectedPosition(newPosition: Int, conversation: DeepseekConversation) {
        if (curSelectPosition != newPosition) {
            selectedConversation = conversation

            val lastSelectedPosition = curSelectPosition
            curSelectPosition = newPosition

            notifyItemChanged(lastSelectedPosition)
            notifyItemChanged(curSelectPosition)
        }
    }

    class ViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root)

    fun handleTimestamp(timestamp: Long): String {
        val timeDiff = System.currentTimeMillis() - timestamp

        return if (timeDiff > 7.days.ms()) {
            timestamp.toTimeStr()
        } else if (timeDiff > 1.days.ms()) {
            "${timeDiff / 1.days.ms()}天前"
        } else if (timeDiff > 1.hours.ms()) {
            "${timeDiff / 1.hours.ms()}小时前"
        } else if (timeDiff > 1.minutes.ms()) {
            "${timeDiff / 1.minutes.ms()}分钟前"
        } else {
            "刚刚"
        }
    }

    private fun Duration.ms() = inWholeMilliseconds
    private fun Long.toTimeStr() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(this))
}