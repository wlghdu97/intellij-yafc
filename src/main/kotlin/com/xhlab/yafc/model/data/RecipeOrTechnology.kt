package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.util.toSet

sealed interface RecipeOrTechnology : FactorioObject {
    val crafters: List<EntityCrafter>
    val ingredients: List<Ingredient>
    val products: List<Product>
    val modules: List<Item>
    val sourceEntity: Entity?
    val mainProduct: Goods?
    val time: Float
    val enabled: Boolean
    val hidden: Boolean
    val flags: RecipeFlags

    override val sortingOrder: FactorioObjectSortOrder
        get() = FactorioObjectSortOrder.RECIPES
    override val type: String
        get() = "Recipe"

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        if (ingredients.isNotEmpty()) {
            temp.clear()
            for (ingredient in ingredients) {
                val variants = ingredient.variants
                if (variants != null) {
                    collector.addObject(variants, DependencyList.Flag.INGREDIENT_VARIANT.toSet())
                } else {
                    temp.add(ingredient.goods)
                }
            }
            if (temp.isNotEmpty()) {
                collector.addObject(temp, DependencyList.Flag.INGREDIENT.toSet())
            }
        }

        collector.addObject(crafters, DependencyList.Flag.CRAFTING_ENTITY.toSet())

        sourceEntity?.let {
            collector.addId(listOf(it.id), DependencyList.Flag.SOURCE_ENTITY.toSet())
        }
    }

    fun canFit(itemInputs: Int, fluidInputs: Int, slots: List<Goods>): Boolean {
        var mutableItemInputs = itemInputs
        var mutableFluidInputs = fluidInputs

        for (ingredient in ingredients) {
            if (ingredient.goods is Item && --mutableItemInputs < 0) return false
            if (ingredient.goods is Fluid && --mutableFluidInputs < 0) return false
            if (slots.isNotEmpty() && !slots.contains(ingredient.goods)) return false
        }

        return true
    }
}
