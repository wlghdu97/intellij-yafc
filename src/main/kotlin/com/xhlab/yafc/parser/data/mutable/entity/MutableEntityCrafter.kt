package com.xhlab.yafc.parser.data.mutable.entity

import com.xhlab.yafc.parser.data.mutable.MutableGoods
import com.xhlab.yafc.parser.data.mutable.MutableRecipeOrTechnology

internal abstract class MutableEntityCrafter : MutableEntityWithModules() {
    abstract var itemInputs: Int
    abstract var fluidInputs: Int // fluid inputs for recipe, not including power
    abstract var inputs: List<MutableGoods>
    abstract var recipes: List<MutableRecipeOrTechnology>
    open var craftingSpeed: Float = 1f
    abstract var productivity: Float
}
