package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.IFactorioObjectWrapper
import com.xhlab.yafc.model.data.TemperatureRange

internal data class MutableIngredient constructor(
    val goods: MutableGoods,
    override val amount: Float,
    val variants: List<MutableGoods>? = null
) : IFactorioObjectWrapper {

    val temperature: TemperatureRange = if (goods is MutableFluid) {
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

    fun containsVariant(product: MutableGoods): Boolean {
        if (goods == product) {
            return true
        }
        if (variants != null) {
            return variants.indexOf(product) >= 0
        }

        return false
    }
}
