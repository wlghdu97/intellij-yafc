package com.xhlab.yafc.model.data

internal data class MutableIngredient constructor(
    override var goods: MutableGoods,
    override val amount: Float,
    override var variants: List<MutableGoods>? = null
) : Ingredient {
    override var temperature: TemperatureRange = super.temperature

    fun containsVariant(product: Goods): Boolean {
        if (goods == product) {
            return true
        }
        val variants = variants
        if (variants != null) {
            return variants.indexOf(product) >= 0
        }

        return false
    }
}
