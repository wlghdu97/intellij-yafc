package com.xhlab.yafc.model

import com.google.gson.JsonParseException
import java.util.Comparator

abstract class ErrorCollector {
    protected val allErrors = hashMapOf<Error, Int>()
    protected var severity: ErrorSeverity = ErrorSeverity.NONE

    open fun appendError(message: String, severity: ErrorSeverity) {
        if (severity > this.severity) {
            this.severity = severity
        }
        val key = Error(message, severity)
        val count = allErrors[key] ?: 1
        allErrors[key] = count
    }

    open fun appendException(throwable: Throwable, message: String, severity: ErrorSeverity) {
        val msg = "$message: " + if (throwable is JsonParseException) {
            "unexpected or invalid json"
        } else {
            throwable.message
        }
        appendError(msg, severity)
    }

    protected fun getArrErrors(): List<Error> {
        return allErrors.toSortedMap(Comparator.comparing { it.severity })
            .map { (error, count) ->
                if (count == 1) error else error.copy(message = "${error.message} ($count)")
            }
    }

    /**
     * prints all containing errors and warnings and clears all printed ones.
     */
    abstract fun flush()

    protected fun reset() {
        allErrors.clear()
        severity = ErrorSeverity.NONE
    }

    data class Error(val message: String, val severity: ErrorSeverity)
}
