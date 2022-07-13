package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityCrafter

abstract class RecipeOrTechnology : FactorioObject() {

    abstract val crafters: List<EntityCrafter>
    abstract val ingredients: List<Ingredient>
    abstract val products: List<Product>
    open val modules: List<Item> = emptyList()
    abstract val sourceEntity: Entity?
    abstract val mainProduct: Goods?
    abstract val time: Float
    abstract val enabled: Boolean
    abstract val hidden: Boolean
    abstract val flags: RecipeFlags?

    override val type: String = "Recipe"

    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.RECIPES

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        if (ingredients.isNotEmpty()) {
            temp.clear()
            for (ingredient in ingredients) {
                if (ingredient.variants != null) {
                    collector.addObject(ingredient.variants, DependencyList.Flags.INGREDIENT_VARIANT)
                } else {
                    temp.add(ingredient.goods)
                }
            }
            if (temp.isNotEmpty()) {
                collector.addObject(temp, DependencyList.Flags.INGREDIENT)
            }
        }

        collector.addObject(crafters, DependencyList.Flags.CRAFTING_ENTITY)

        sourceEntity?.let {
            collector.addId(listOf(it.id!!), DependencyList.Flags.SOURCE_ENTITY)
        }
    }

    fun canFit(itemInputs: Int, fluidInputs: Int, slots: List<Goods>?): Boolean {
        var mutableItemInputs = itemInputs
        var mutableFluidInputs = fluidInputs

        for (ingredient in ingredients) {
            if (ingredient.goods is Item && --mutableItemInputs < 0) return false
            if (ingredient.goods is Fluid && --mutableFluidInputs < 0) return false
            if (slots != null && !slots.contains(ingredient.goods)) return false
        }

        return true
    }
}
