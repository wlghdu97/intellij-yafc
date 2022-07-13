package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.data.DataUtils
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.IFactorioObjectWrapper
import com.xhlab.yafc.model.data.UnitOfMeasure

internal data class MutableProduct(
    var goods: MutableGoods,
    var amountMin: Float,
    var amountMax: Float,
    override var amount: Float, // This is average amount including probability and range
    var productivityAmount: Float,
    var probability: Float
) : IFactorioObjectWrapper {

    constructor(goods: MutableGoods, amount: Float) : this(goods, amount, amount, amount, amount, amount)

    constructor(goods: MutableGoods, min: Float, max: Float, probability: Float) : this(
        goods = goods,
        amountMin = min,
        amountMax = max,
        amount = probability * (min + max) / 2,
        productivityAmount = probability * (min + max) / 2,
        probability = probability
    )

    fun setCatalyst(catalyst: Float) {
        val catalyticMin = amountMin - catalyst
        val catalyticMax = amountMax - catalyst
        productivityAmount = when {
            (catalyticMax <= 0) -> {
                0f
            }

            (catalyticMin >= 0f) -> {
                (catalyticMin + catalyticMax) * 0.5f * probability
            }

            else -> {
                // TODO super duper rare case, might not be precise
                probability * catalyticMax * catalyticMax * 0.5f / (catalyticMax - catalyticMin)
            }
        }
    }

    fun getAmount(productivityBonus: Float) = amount + productivityBonus * productivityAmount

    val isSimple: Boolean
        get() = amountMin == amountMax && probability == 1f

    override val target: FactorioObject
        get() = goods

    override val text: String
        get() {
            var text = goods.locName
            if (amountMin != 1f || amountMax != 1f) {
                text = DataUtils.formatAmount(amountMax, UnitOfMeasure.NONE) + "x " + text
                if (amountMin != amountMax) {
                    text = DataUtils.formatAmount(amountMin, UnitOfMeasure.NONE) + "-" + text
                }
            }
            if (probability != 1f) {
                text = DataUtils.formatAmount(probability, UnitOfMeasure.PERCENT) + " " + text
            }

            return text ?: ""
        }
}
