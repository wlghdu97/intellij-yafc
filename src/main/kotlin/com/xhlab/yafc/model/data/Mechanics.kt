package com.xhlab.yafc.model.data

sealed class Mechanics : Recipe {
    abstract val source: FactorioObject

    final override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.MECHANICS
    final override val type: String = "Mechanics"
}
