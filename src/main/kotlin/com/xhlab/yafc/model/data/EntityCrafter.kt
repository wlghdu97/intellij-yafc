package com.xhlab.yafc.model.data

sealed interface EntityCrafter : EntityWithModules {
    val itemInputs: Int
    val fluidInputs: Int // fluid inputs for recipe, not including power
    val inputs: List<Goods>
    val recipes: List<RecipeOrTechnology>
    val craftingSpeed: Float
    val productivity: Float
}
