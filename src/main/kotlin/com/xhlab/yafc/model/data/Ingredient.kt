package com.xhlab.yafc.model.data

data class Ingredient constructor(
    val goods: Goods,
    override val amount: Float,
    val variants: List<Goods>?
) : IFactorioObjectWrapper {

    val temperature: TemperatureRange = if (goods is Fluid) {
        goods.temperatureRange
    } else {
        TemperatureRange.Any
    }

    override val text: String
        get() {
            var text = goods.locName ?: ""
            if (amount != 1f) {
                text = "${amount}x $text"
            }
            if (!temperature.isAny()) {
                text += " ($temperature)"
            }

            return text
        }

    override val target: FactorioObject = goods

    fun containsVariant(product: Goods): Boolean {
        if (goods == product) {
            return true
        }
        if (variants != null) {
            return variants.indexOf(product) >= 0
        }

        return false
    }
}
