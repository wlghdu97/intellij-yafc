package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector

abstract class Technology : RecipeOrTechnology() { // Technology is very similar to recipe
    abstract val count: Float // TODO support formula count
    open val prerequisites: List<Technology> = emptyList()
    open val unlockRecipes: List<Recipe> = emptyList()
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.TECHNOLOGIES
    override val type: String = "Technology"

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        super.getDependencies(collector, temp)
        if (prerequisites.isNotEmpty()) {
            collector.addObject(prerequisites, DependencyList.Flags.TECHNOLOGY_PREREQUISITES)
        }
        if (hidden && !enabled) {
            collector.addId(emptyList(), DependencyList.Flags.HIDDEN)
        }
    }
}
