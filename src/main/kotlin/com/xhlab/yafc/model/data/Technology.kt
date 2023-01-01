package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.util.toSet

sealed class Technology : RecipeOrTechnology { // Technology is very similar to recipe
    abstract val count: Float // TODO support formula count
    abstract val prerequisites: List<Technology>
    abstract val unlockRecipes: List<Recipe>

    final override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.TECHNOLOGIES
    final override val type: String = "Technology"

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        super.getDependencies(collector, temp)
        if (prerequisites.isNotEmpty()) {
            collector.addObject(prerequisites, DependencyList.Flag.TECHNOLOGY_PREREQUISITES.toSet())
        }
        if (hidden && !enabled) {
            collector.addId(emptyList(), DependencyList.Flag.HIDDEN.toSet())
        }
    }
}
