package com.xhlab.yafc.parser

import com.intellij.util.messages.Topic

interface ParserProgressChangeListener {
    fun progressChanged(title: String, description: String)
    fun currentLoadingModChanged(mod: String?)

    companion object {
        val TOPIC_PARSER_PROGRESS_CHANGE =
            Topic.create("YAFC_PARSER_PROGRESS_CHANGE_TOPIC", ParserProgressChangeListener::class.java)
    }
}
