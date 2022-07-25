package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.RecipeOrTechnology
import com.xhlab.yafc.model.data.Technology

internal abstract class MutableRecipe(name: String) : MutableRecipeOrTechnology(name) {
    abstract var fuelResult: MutableItem?
    abstract var recipe: RecipeOrTechnology?
    abstract var technologyUnlock: List<Technology>

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

    fun canAcceptModule(module: MutableItem) = modules.contains(module)
}
