package com.xhlab.yafc.parser

import com.twelvemonkeys.io.LittleEndianDataInputStream
import org.luaj.vm2.LuaValue

class FactorioPropertyTree {

    fun readModSettings(inputStream: LittleEndianDataInputStream): LuaValue {
        inputStream.readLong()
        inputStream.readBoolean()
        return readAny(inputStream)
    }

    private fun readSpaceOptimizedUint(inputStream: LittleEndianDataInputStream): Int {
        val b = inputStream.readUnsignedByte()
        if (b < 255) {
            return b
        }

        return inputStream.readInt()
    }

    private fun readString(inputStream: LittleEndianDataInputStream): String {
        if (inputStream.readBoolean()) {
            return ""
        }

        val len = readSpaceOptimizedUint(inputStream)
        val bytes = ByteArray(len).apply {
            inputStream.read(this)
        }

        return bytes.toString(Charsets.UTF_8)
    }

    private fun readAny(inputStream: LittleEndianDataInputStream): LuaValue {
        val type = inputStream.readByte()
        inputStream.readByte()
        return when (type) {
            0.toByte() -> {
                LuaValue.NIL
            }
            1.toByte() -> {
                LuaValue.valueOf(inputStream.readBoolean())
            }
            2.toByte() -> {
                LuaValue.valueOf(inputStream.readDouble())
            }
            3.toByte() -> {
                LuaValue.valueOf(readString(inputStream))
            }
            4.toByte() -> {
                val count = inputStream.read()
                val arr = LuaValue.tableOf()
                for (i in 0 until count) {
                    readString(inputStream)
                    arr[i + 1] = readAny(inputStream)
                }

                return arr
            }
            5.toByte() -> {
                val count = inputStream.readInt()
                val table = LuaValue.tableOf()
                repeat(count) {
                    val key = readString(inputStream)
                    table[key] = readAny(inputStream)
                }

                return table
            }
            else -> {
                throw IllegalArgumentException("Unknown type")
            }
        }
    }
}
