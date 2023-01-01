package com.xhlab.yafc.parser

inline fun <reified T> YAFCLogger.debug(message: String) {
    debug(T::class.java, message)
}

inline fun <reified T> YAFCLogger.info(message: String) {
    info(T::class.java, message)
}

interface YAFCLogger {
    fun debug(cl: Class<*>, message: String)
    fun info(cl: Class<*>, message: String)
}
