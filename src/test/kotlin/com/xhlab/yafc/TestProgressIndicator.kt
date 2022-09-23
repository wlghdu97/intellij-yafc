package com.xhlab.yafc

import com.xhlab.yafc.parser.ProgressTextIndicator

class TestProgressIndicator : ProgressTextIndicator {

    override fun setText(text: String, description: String) {
        println("$text : $description")
    }

    override fun setText(description: String) {
        println(description)
    }
}
