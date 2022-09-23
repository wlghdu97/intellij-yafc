package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector

sealed interface Entity : FactorioObject {
    val loot: List<Product>
    val mapGenerated: Boolean
    val mapGenDensity: Float
    val power: Float
    val energy: EntityEnergy?
    val itemsToPlace: List<Item>
    val size: Int

    override val sortingOrder: FactorioObjectSortOrder
        get() = FactorioObjectSortOrder.ENTITIES
    override val type: String
        get() = "Entity"

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        energy?.let {
            collector.addObject(it.fuels, DependencyList.Flags.FUEL)
        }
        if (mapGenerated) {
            return
        }
        collector.addObject(itemsToPlace, DependencyList.Flags.ITEM_TO_PLACE)
    }
}
