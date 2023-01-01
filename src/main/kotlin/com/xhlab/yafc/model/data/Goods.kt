package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.util.toSet

// Abstract base for anything that can be produced or consumed by recipes (etc.)
sealed interface Goods : FactorioObject {
    val fuelValue: Float
    val isPower: Boolean
    val production: List<Recipe>
    val usages: List<Recipe>
    val miscSources: List<FactorioObject>
    val fuelFor: List<Entity>
    val flowUnitOfMeasure: UnitOfMeasure

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        collector.addObject(production + miscSources, DependencyList.Flag.SOURCE.toSet())
    }
}

