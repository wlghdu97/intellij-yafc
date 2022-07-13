package com.xhlab.yafc.model.data

class Product : IFactorioObjectWrapper {

    val goods: Goods
    val amountMin: Float
    val amountMax: Float
    override val amount: Float // This is average amount including probability and range
    var productivityAmount: Float
    val probability: Float

    constructor(goods: Goods, amount: Float) {
        this.goods = goods
        this.amountMin = amount
        this.amountMax = amount
        this.amount = amount
        this.productivityAmount = amount
        this.probability = amount
    }

    constructor(goods: Goods, min: Float, max: Float, probability: Float) {
        this.goods = goods
        this.amountMin = min
        this.amountMax = max
        this.probability = probability
        val amount = probability * (min + max) / 2
        this.amount = amount
        this.productivityAmount = amount
    }

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
