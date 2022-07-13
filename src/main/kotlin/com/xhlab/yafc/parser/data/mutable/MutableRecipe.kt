package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityCrafter
import javax.swing.Icon

internal data class MutableRecipe(
    override val name: String,
    override var factorioType: String? = null,
    override var originalName: String? = null,
    override var typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override var icon: Icon? = null,
    override var id: FactorioId? = null,
    var fuelResult: MutableItem? = null,
    var recipe: RecipeOrTechnology? = null,
    var technologyUnlock: List<Technology> = emptyList(),
    override val crafters: List<EntityCrafter> = emptyList(),
    override val ingredients: List<Ingredient> = emptyList(),
    override val products: List<MutableProduct> = emptyList(),
    override val sourceEntity: Entity? = null,
    override val mainProduct: Goods? = null,
    override val time: Float = 0f,
    override val enabled: Boolean = false,
    override val hidden: Boolean = false,
    override val flags: RecipeFlags? = null
) : MutableRecipeOrTechnology(name) {

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
