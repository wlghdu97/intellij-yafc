package com.xhlab.yafc.ide.model

import com.intellij.openapi.diagnostic.Logger
import com.xhlab.yafc.parser.YAFCLogger

class IntellijYAFCLogger : YAFCLogger {

    override fun debug(cl: Class<*>, message: String) {
        Logger.getInstance(cl).debug(message)
    }

    override fun info(cl: Class<*>, message: String) {
        Logger.getInstance(cl).info(message)
    }
}
