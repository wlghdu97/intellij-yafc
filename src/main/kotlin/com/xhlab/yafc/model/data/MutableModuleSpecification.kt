package com.xhlab.yafc.model.data

internal data class MutableModuleSpecification(
    override var consumption: Float = 0f,
    override var speed: Float = 0f,
    override var productivity: Float = 0f,
    override var pollution: Float = 0f
) : ModuleSpecification {
    // excluded from equal check
    override var limitation: List<MutableRecipeImpl> = emptyList()
}
