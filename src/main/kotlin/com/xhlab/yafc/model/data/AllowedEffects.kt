package com.xhlab.yafc.model.data

enum class AllowedEffects(val value: Int) {
    SPEED(1 shl 0),
    PRODUCTIVITY(1 shl 1),
    CONSUMPTION(1 shl 2),
    POLLUTION(1 shl 3),

    ALL(SPEED.value or PRODUCTIVITY.value or CONSUMPTION.value or POLLUTION.value),
    NONE(0)
}
