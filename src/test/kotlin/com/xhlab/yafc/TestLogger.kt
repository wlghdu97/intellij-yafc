package com.xhlab.yafc

import com.xhlab.yafc.parser.YAFCLogger

class TestLogger : YAFCLogger {

    override fun debug(cl: Class<*>, message: String) {
        printlnLog(cl, message)
    }

    override fun info(cl: Class<*>, message: String) {
        printlnLog(cl, message)
    }

    private fun printlnLog(cl: Class<*>, message: String) {
        println("${cl::class.simpleName} : $message")
    }
}
