package com.smybs0.deepseekchat.utils

import android.content.Context
import android.text.Spanned
import io.noties.markwon.Markwon
import io.noties.markwon.PrecomputedTextSetterCompat
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import java.util.concurrent.Executors

internal lateinit var markwon: Markwon

internal fun initMarkwon(context: Context) {
    markwon = Markwon.builder(context)
        .usePlugin(CorePlugin.create())
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(JLatexMathPlugin.create(16f))
        .build()
}

internal fun getMarkdown(content: String): Spanned {
    return markwon.toMarkdown(content)
}

internal val markdownCodeRegex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
internal val markdownTableRegex =
    Regex("(?:\\|.*\\|[\\s]*\\n)+\\|[\\s\\-:|]+\\|[\\s]*\\n(?:\\|.*\\|[\\s]*\\n?)+")
