package com.xhlab.yafc.model.data

data class ModuleSpecification(
    val consumption: Float = 0f,
    val speed: Float = 0f,
    val productivity: Float = 0f,
    val pollution: Float = 0f,
    val limitation: List<Recipe> = emptyList()
)
