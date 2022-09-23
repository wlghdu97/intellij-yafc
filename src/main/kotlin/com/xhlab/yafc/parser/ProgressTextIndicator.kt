package com.xhlab.yafc.parser

interface ProgressTextIndicator {
    fun setText(text: String, description: String)
    fun setText(description: String)
}
