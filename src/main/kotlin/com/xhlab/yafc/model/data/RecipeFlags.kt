package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.util.EnumFlag
import java.util.*

typealias RecipeFlags = EnumSet<RecipeFlag>

enum class RecipeFlag(override val value: Int) : EnumFlag {
    USES_MINING_PRODUCTIVITY(1 shl 0),
    USES_FLUID_TEMPERATURE(1 shl 2),
    SCALE_PRODUCTION_WITH_POWER(1 shl 3),
    LIMITED_BY_TICK_RATE(1 shl 4);
}
