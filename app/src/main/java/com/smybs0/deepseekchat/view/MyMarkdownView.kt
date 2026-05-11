package com.smybs0.deepseekchat.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smybs0.deepseekchat.R
import com.smybs0.deepseekchat.utils.getMarkdown
import com.smybs0.deepseekchat.utils.markdownCodeRegex
import com.smybs0.deepseekchat.utils.markdownTableRegex

internal class MyMarkdownView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private lateinit var blocks: ArrayList<MarkdownBlock>
    private val markdownAdapter by lazy { MarkdownAdapter() }

    init {
        layoutManager = LinearLayoutManager(context)
    }

    var text: CharSequence? = null
        set(value) {
            Log.d("TAG", "111: $value")

            if (text == value) return
            else {
                field = value

                if (value == null) {
                    if (::blocks.isInitialized) {
                        blocks.clear()
                        markdownAdapter.notifyDataSetChanged()
                    }
                } else {
                    val newBlocks = splitMarkdownBlock(value)

                    if (!::blocks.isInitialized) {
                        blocks = ArrayList(newBlocks)
                        adapter = markdownAdapter
                        markdownAdapter.notifyItemRangeInserted(0, blocks.size)
                    } else {
                        newBlocks.forEachIndexed { index, block ->
                            if (blocks.size > index) {
                                if (blocks[index] != block) {
                                    blocks[index] = block
                                    markdownAdapter.notifyItemChanged(index)
                                }
                            } else {
                                blocks.add(block)
                            }
                        }
                        if (newBlocks.size > blocks.size) {
                            markdownAdapter.notifyItemRangeInserted(
                                blocks.size,
                                newBlocks.size - blocks.size
                            )
                        } else if (newBlocks.size < blocks.size) {
                            markdownAdapter.notifyItemRangeRemoved(
                                newBlocks.size,
                                blocks.size - newBlocks.size
                            )
                        }
                    }
                }
            }
        }


    enum class MarkdownBlockType {
        TABLE, CODE, OTHER
    }

    data class MarkdownBlock(
        val text: String,
        val type: MarkdownBlockType,
    )

    fun splitMarkdownBlock(text: CharSequence): List<MarkdownBlock> {
        val blocks = ArrayList<Pair<IntRange, MarkdownBlockType>>()
        var curPosition = 0
        val len = text.length

        val codeBlocks = markdownCodeRegex.findAll(text).toList()
        var idx1 = 0
        val tableBlocks = markdownTableRegex.findAll(text).toList()
        var idx2 = 0

        while (curPosition < len) {
            var minRange: IntRange? = null
            var type: MarkdownBlockType = MarkdownBlockType.OTHER

            when {
                idx1 < codeBlocks.size && idx2 < tableBlocks.size -> {
                    val codeRange = codeBlocks[idx1].range
                    val tableRange = tableBlocks[idx2].range

                    if (codeRange.first < tableRange.first) {
                        minRange = codeRange
                        type = MarkdownBlockType.CODE
                        idx1++
                    } else {
                        minRange = tableRange
                        type = MarkdownBlockType.TABLE
                        idx2++
                    }
                }

                idx1 < codeBlocks.size -> {
                    minRange = codeBlocks[idx1].range
                    type = MarkdownBlockType.CODE
                    idx1++
                }

                idx2 < tableBlocks.size -> {
                    minRange = tableBlocks[idx2].range
                    type = MarkdownBlockType.TABLE
                    idx2++
                }
            }

            if (minRange != null) {
                if (curPosition < minRange.first) {
                    blocks.add(curPosition until minRange.first to MarkdownBlockType.OTHER)
                }
                blocks.add(minRange to type)
                curPosition = minRange.last + 1
            } else {
                if (curPosition < len) {
                    blocks.add(curPosition until len to MarkdownBlockType.OTHER)
                }
                break
            }
        }

        return blocks.map { (range, type) -> MarkdownBlock(text.substring(range), type) }.apply {
            forEach { block -> Log.d("TAG", "$block") }
        }
    }


    inner class MarkdownAdapter() : Adapter<MarkdownViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkdownViewHolder {
            val view = when (MarkdownBlockType.entries[viewType]) {
                MarkdownBlockType.TABLE ->
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_markdown_table, parent, false)

                MarkdownBlockType.CODE ->
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_markdown_code, parent, false)

                MarkdownBlockType.OTHER ->
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_markdown_normal, parent, false)
            }

            return MarkdownViewHolder(view)
        }

        override fun onBindViewHolder(holder: MarkdownViewHolder, position: Int) {
            val block = blocks[position]

            if (block.type == MarkdownBlockType.TABLE) {
                holder.itemView.findViewById<TableLayout>(R.id.table_layout)?.run {
                    removeAllViews()

                    block.text.trim().split("\n").forEach { row ->
                        val tableRow = TableRow(context)
                        row.substring(row.indexOf('|'), row.lastIndexOf('|')).split("|")
                            .forEach { cell ->
                                Log.d("TAG", "onBindViewHolder: cell = $cell")
                                val textView = TextView(context).apply {
                                    setPadding(16, 12, 16, 12)
                                    text = cell
                                    maxWidth = (200 * resources.displayMetrics.density).toInt()
                                }
                                tableRow.addView(textView)
                            }
                        addView(tableRow)
                    }
                }

            } else {
                holder.itemView.findViewById<TextView>(R.id.tv_text)
                    ?.run { text = getMarkdown(block.text) }
            }
        }

        override fun getItemCount() = blocks.size

        override fun getItemViewType(position: Int): Int = blocks[position].type.ordinal
    }

    class MarkdownViewHolder(itemView: View) : ViewHolder(itemView)
}
