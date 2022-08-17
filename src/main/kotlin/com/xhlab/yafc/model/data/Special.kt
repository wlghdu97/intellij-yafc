package com.xhlab.yafc.model.data

sealed class Special : Goods {
    abstract val virtualSignal: String

    final override val type: String
        get() = if (isPower) "Power" else "Special"
    final override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.SPECIAL_GOODS
    final override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.PER_SECOND
}
