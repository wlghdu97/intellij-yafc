package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.entity.Entity

// Abstract base for anything that can be produced or consumed by recipes (etc.)
abstract class Goods : FactorioObject() {
    abstract val fuelValue: Float
    abstract val isPower: Boolean
    abstract val production: List<Recipe>
    abstract val usages: List<Recipe>
    abstract val miscSources: List<FactorioObject>
    abstract val fuelFor: List<Entity>
    abstract val flowUnitOfMeasure: UnitOfMeasure

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        collector.addObject(production + miscSources, DependencyList.Flags.SOURCE)
    }

//    open fun hasSpentFuel(spent: Item): Boolean {
//        spent = null
//        return false
//    }
}

