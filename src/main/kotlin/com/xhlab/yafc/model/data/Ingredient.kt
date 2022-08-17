package com.xhlab.yafc.model.data

sealed interface Ingredient : IFactorioObjectWrapper {
    val goods: Goods
    val variants: List<Goods>?

    val temperature: TemperatureRange
        get() = (goods as? Fluid)?.temperatureRange ?: TemperatureRange.Any

    override val text: String
        get() {
            var text = goods.locName
            if (amount != 1f) {
                text = "${amount}x $text"
            }
            if (!temperature.isAny()) {
                text += " ($temperature)"
            }

            return text
        }

    override val target: FactorioObject
        get() = goods
}
