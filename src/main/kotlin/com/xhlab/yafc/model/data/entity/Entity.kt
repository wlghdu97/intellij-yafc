package com.xhlab.yafc.model.data.entity

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.FactorioObjectSortOrder
import com.xhlab.yafc.model.data.Item
import com.xhlab.yafc.model.data.Product

abstract class Entity : FactorioObject() {
    open val loot: List<Product> = emptyList()
    abstract val mapGenerated: Boolean
    abstract val mapGenDensity: Float
    abstract val power: Float
    abstract val energy: EntityEnergy?
    abstract val itemToPlace: List<Item>
    abstract val size: Int

    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.ENTITIES
    override val type: String = "Entity"

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        energy?.let {
            collector.addObject(it.fuels, DependencyList.Flags.FUEL)
        }
        if (mapGenerated) {
            return
        }
        collector.addObject(itemToPlace, DependencyList.Flags.ITEM_TO_PLACE)
    }
}
