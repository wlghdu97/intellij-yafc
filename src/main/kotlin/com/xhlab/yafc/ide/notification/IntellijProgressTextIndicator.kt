package com.xhlab.yafc.ide.notification

import com.intellij.openapi.progress.ProgressIndicator
import com.xhlab.yafc.parser.ProgressTextIndicator

class IntellijProgressTextIndicator constructor(private val progress: ProgressIndicator) : ProgressTextIndicator {

    override fun setText(text: String, description: String) {
        progress.text = text
        progress.text2 = description
    }

    override fun setText(description: String) {
        progress.text2 = description
    }
}
