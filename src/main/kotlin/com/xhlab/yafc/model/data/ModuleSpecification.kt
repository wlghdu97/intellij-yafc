package com.xhlab.yafc.model.data

sealed interface ModuleSpecification {
    val consumption: Float
    val speed: Float
    val productivity: Float
    val pollution: Float
    val limitation: List<Recipe>
}
