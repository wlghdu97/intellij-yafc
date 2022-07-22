package com.xhlab.yafc.parser.data.mutable

internal data class MutableModuleSpecification(
    var consumption: Float = 0f,
    var speed: Float = 0f,
    var productivity: Float = 0f,
    var pollution: Float = 0f,
    var limitation: List<MutableRecipeImpl> = emptyList()
)
