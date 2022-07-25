package com.xhlab.yafc.model.data

enum class RecipeFlags(val value: Int) {
    USES_MINING_PRODUCTIVITY(1 shl 0),
    USES_FLUID_TEMPERATURE(1 shl 2),
    SCALE_PRODUCTION_WITH_POWER(1 shl 3),
    LIMITED_BY_TICK_RATE(1 shl 4);

    infix fun or(other: RecipeFlags?): RecipeFlags? {
        return if (other == null) {
            this
        } else {
            val newValue = value or other.value
            values().find { it.value == newValue }
        }
    }
}
