package com.xhlab.yafc.parser

interface ParserProgressChangeListener {
    fun progressChanged(title: String, description: String)
    fun currentLoadingModChanged(mod: String?)
}
