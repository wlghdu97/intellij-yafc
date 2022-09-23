package com.xhlab.yafc.model.data

sealed class Item : Goods {
    abstract val fuelResult: Item?
    abstract val stackSize: Int
    abstract val placeResult: Entity?
    abstract val module: ModuleSpecification?

    final override val isPower: Boolean = false
    final override val type: String = "Item"
    final override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.ITEMS
    final override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.ITEM_PER_SECOND
}
