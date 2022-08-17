package com.xhlab.yafc.model.data

internal sealed interface MutableRecipe : Recipe, MutableRecipeOrTechnology {
    override var technologyUnlock: List<MutableTechnology>
}
