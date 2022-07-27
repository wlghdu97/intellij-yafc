package com.xhlab.yafc.model.data

enum class AllowedEffects(val value: Int) {
    SPEED(1 shl 0),
    PRODUCTIVITY(1 shl 1),
    CONSUMPTION(1 shl 2),
    POLLUTION(1 shl 3),

    ALL(SPEED.value or PRODUCTIVITY.value or CONSUMPTION.value or POLLUTION.value),
    NONE(0);

    infix fun or(other: AllowedEffects): AllowedEffects {
        return fromValue(value or other.value)
    }

    infix fun xor(other: AllowedEffects): AllowedEffects {
        return fromValue(value xor other.value)
    }

    companion object {
        fun fromString(type: String): AllowedEffects {
            return when (type.lowercase()) {
                "speed" -> SPEED
                "productivity" -> PRODUCTIVITY
                "consumption" -> CONSUMPTION
                "pollution" -> POLLUTION
                "all" -> ALL
                else -> NONE
            }
        }

        private fun fromValue(value: Int): AllowedEffects {
            return values().find { it.value == value } ?: NONE
        }
    }
}
