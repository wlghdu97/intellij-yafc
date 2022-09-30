package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.util.EnumFlag
import java.util.*

typealias AllowedEffects = EnumSet<AllowedEffect>

enum class AllowedEffect(override val value: Int) : EnumFlag {
    SPEED(1 shl 0),
    PRODUCTIVITY(1 shl 1),
    CONSUMPTION(1 shl 2),
    POLLUTION(1 shl 3);

    companion object {
        fun fromString(type: String): AllowedEffects {
            return when (type.lowercase()) {
                "speed" -> EnumSet.of(SPEED)
                "productivity" -> EnumSet.of(PRODUCTIVITY)
                "consumption" -> EnumSet.of(CONSUMPTION)
                "pollution" -> EnumSet.of(POLLUTION)
                "all" -> EnumSet.allOf(AllowedEffect::class.java)
                else -> EnumSet.noneOf(AllowedEffect::class.java)
            }
        }
    }
}
