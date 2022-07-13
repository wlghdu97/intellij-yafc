package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector

abstract class Recipe : RecipeOrTechnology() {
    abstract val recipe: RecipeOrTechnology?
    abstract val technologyUnlock: List<Technology>

    fun hasIngredientVariants(): Boolean {
        for (ingredient in ingredients) {
            if (ingredient.variants != null) {
                return true
            }
        }

        return false
    }

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        super.getDependencies(collector, temp)
        if (!enabled) {
            collector.addObject(technologyUnlock, DependencyList.Flags.TECHNOLOGY_UNLOCK)
        }
    }

    fun isProductivityAllowed(): Boolean {
        for (module in modules) {
            if (module.module?.productivity != 0f) {
                return true
            }
        }

        return false
    }

    fun canAcceptModule(module: Item) = modules.contains(module)
}
