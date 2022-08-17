package com.xhlab.yafc.model.data

internal sealed interface MutableEntityCrafter : EntityCrafter, MutableEntityWithModules {
    override var itemInputs: Int
    override var fluidInputs: Int // fluid inputs for recipe, not including power
    override var inputs: List<MutableGoods>
    override var recipes: List<MutableRecipeOrTechnology>
    override var craftingSpeed: Float
    override var productivity: Float
}
