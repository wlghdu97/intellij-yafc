package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.FactorioObjectSortOrder
import com.xhlab.yafc.model.data.RecipeFlags
import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityCrafter

internal abstract class MutableRecipeOrTechnology : MutableFactorioObject() {
    abstract var crafters: List<EntityCrafter>
    abstract var ingredients: List<MutableIngredient>
    abstract var products: List<MutableProduct>
    open var modules: List<MutableItem> = emptyList()
    abstract var sourceEntity: Entity?
    abstract var mainProduct: MutableGoods?
    abstract var time: Float
    abstract var enabled: Boolean
    abstract var hidden: Boolean
    abstract var flags: RecipeFlags?

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

    fun canFit(itemInputs: Int, fluidInputs: Int, slots: List<MutableGoods>?): Boolean {
        var mutableItemInputs = itemInputs
        var mutableFluidInputs = fluidInputs

        for (ingredient in ingredients) {
            if (ingredient.goods is MutableItem && --mutableItemInputs < 0) return false
            if (ingredient.goods is MutableFluid && --mutableFluidInputs < 0) return false
            if (slots != null && !slots.contains(ingredient.goods)) return false
        }

        return true
    }
}
