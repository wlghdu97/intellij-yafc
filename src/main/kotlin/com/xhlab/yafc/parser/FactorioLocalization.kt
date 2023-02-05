package com.xhlab.yafc.parser

import java.io.BufferedReader

object FactorioLocalization {

    // TODO: localization kv cache
    private val keys = hashMapOf<String, String>()

    fun parse(reader: BufferedReader) {
        var category = ""
        while (reader.ready()) {
            val line = reader.readLine()?.trim() ?: return
            if (line.startsWith("[") && line.endsWith("]")) {
                category = line.substring(1, line.length - 1)
            } else {
                val idx = line.indexOf('=')
                if (idx == -1) {
                    continue
                }
                val key = line.substring(0, idx)
                val value = line.substring(idx + 1, line.length)
                val fullKey = "$category.$key"
                if (!keys.containsKey(fullKey)) {
                    keys[fullKey] = cleanupTags(value)
                }
            }
        }
    }

    private fun cleanupTags(source: String): String {
        var src = source
        while (true) {
            val tagStart = src.indexOf('[')
            if (tagStart == -1) {
                return src.trim()
            }
            val tagEnd = src.indexOf(']', tagStart)
            if (tagEnd == -1) {
                return src.trim()
            }

            src = src.removeRange(tagStart, tagEnd + 1)
        }
    }

    fun localize(key: String): String? {
        val value = keys[key]
        if (value != null) {
            return value
        }

        val lastDash = key.lastIndexOf('-')
        val level = key.substring(lastDash + 1).toIntOrNull()
        if (lastDash != -1 && level != null) {
            val valueWithoutLevel = keys[key.substring(0, lastDash)]
            if (valueWithoutLevel != null) {
                return "$valueWithoutLevel $level"
            }
        }

        return null
    }
}
