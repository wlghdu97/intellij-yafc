package com.xhlab.yafc.parser.data.mutable

internal data class MutableModuleSpecification(
    val consumption: Float = 0f,
    val speed: Float = 0f,
    val productivity: Float = 0f,
    val pollution: Float = 0f,
    val limitation: List<MutableRecipe> = emptyList()
)
