package com.smybs0.deepseekchat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smybs0.deepseekchat.databinding.ItemMessageBinding
import com.smybs0.deepseekchat.utils.getMarkdown
import com.smybs0.deepseeklib.entity.Message

internal class MessageAdapter(
    private val context: Context,
    private val list: List<Message>,
) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    // 保存 position -> item binding 的映射
    private val bindingMap = HashMap<Int, ItemMessageBinding>()

    init { setHasStableIds(true) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        bindingMap.put(position, holder.binding)

        val message = list[position]
        holder.binding.run {
            if (message.reasonContent.isEmpty()) {
                tvReasonContent.visibility = View.GONE
            }

            role = message.role
            tvContent.text = message.content
            tvReasonContent.text = getMarkdown(message.reasonContent)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun getItemId(position: Int) = position.toLong()

    fun getBinding(position: Int): ItemMessageBinding? = bindingMap.get(position)

    class ViewHolder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)
}