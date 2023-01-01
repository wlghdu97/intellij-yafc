package com.xhlab.yafc.model.analysis

import com.xhlab.yafc.model.data.FactorioId
import com.xhlab.yafc.model.util.EnumFlag
import java.util.EnumSet

typealias DependencyListFlags = EnumSet<DependencyList.Flag>

data class DependencyList(
    val flags: DependencyListFlags,
    val elements: Array<FactorioId>
) {
    enum class Flag(override val value: Int) : EnumFlag {
        REQUIRE_EVERYTHING(0x100),
        ONE_TIME_INVESTMENT(0x200),

        INGREDIENT(1 or REQUIRE_EVERYTHING.value),
        CRAFTING_ENTITY(2 or ONE_TIME_INVESTMENT.value),
        SOURCE_ENTITY(3 or ONE_TIME_INVESTMENT.value),
        TECHNOLOGY_UNLOCK(4 or ONE_TIME_INVESTMENT.value),
        SOURCE(5),
        FUEL(6),
        ITEM_TO_PLACE(7),
        TECHNOLOGY_PREREQUISITES(8 or REQUIRE_EVERYTHING.value or ONE_TIME_INVESTMENT.value),
        INGREDIENT_VARIANT(9),
        HIDDEN(10);
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DependencyList

        if (flags != other.flags) return false
        if (!elements.contentEquals(other.elements)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = flags.hashCode()
        result = 31 * result + elements.contentHashCode()
        return result
    }
}

