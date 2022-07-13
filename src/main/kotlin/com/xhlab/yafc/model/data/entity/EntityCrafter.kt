package com.xhlab.yafc.model.data.entity

import com.xhlab.yafc.model.data.Goods
import com.xhlab.yafc.model.data.RecipeOrTechnology

abstract class EntityCrafter : EntityWithModules() {
    abstract val itemInputs: Int
    abstract val fluidInputs: Int // fluid inputs for recipe, not including power
    abstract val inputs: List<Goods>
    abstract val recipes: List<RecipeOrTechnology>
    open val craftingSpeed: Float = 1f
    abstract val productivity: Float
}
