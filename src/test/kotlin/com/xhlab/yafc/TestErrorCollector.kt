package com.xhlab.yafc

import com.xhlab.yafc.model.ErrorCollector
import com.xhlab.yafc.model.ErrorSeverity

class TestErrorCollector : ErrorCollector() {

    override fun appendError(message: String, severity: ErrorSeverity) {
        super.appendError(message, severity)
        println(message)
    }

    override fun appendException(throwable: Throwable, message: String, severity: ErrorSeverity) {
        super.appendException(throwable, message, severity)
        println("$message\n${throwable.stackTraceToString()}")
    }

    override fun flush() {
        if (allErrors.isEmpty()) {
            println("Error collector got no error and warning.")
        } else {
            println(getArrErrors().joinToString("\n") { it.message })
        }
        reset()
    }
}
