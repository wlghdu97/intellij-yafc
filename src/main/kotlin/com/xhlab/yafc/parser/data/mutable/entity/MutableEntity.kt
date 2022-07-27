package com.xhlab.yafc.parser.data.mutable.entity

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.FactorioObjectSortOrder
import com.xhlab.yafc.parser.data.mutable.MutableFactorioObject
import com.xhlab.yafc.parser.data.mutable.MutableItem
import com.xhlab.yafc.parser.data.mutable.MutableProduct

internal abstract class MutableEntity : MutableFactorioObject() {
    open var loot: List<MutableProduct> = emptyList()
    abstract var mapGenerated: Boolean
    abstract var mapGenDensity: Float
    abstract var power: Float
    abstract var energy: MutableEntityEnergy?
    abstract var itemToPlace: List<MutableItem>
    abstract var size: Int

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
