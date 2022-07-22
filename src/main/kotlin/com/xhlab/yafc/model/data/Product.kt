package com.xhlab.yafc.model.data

data class Product(
    val goods: Goods,
    val amountMin: Float,
    val amountMax: Float,
    override val amount: Float, // This is average amount including probability and range
    val productivityAmount: Float,
    val probability: Float
) : IFactorioObjectWrapper {

    constructor(goods: Goods, amount: Float) : this(goods, amount, amount, amount, amount, amount)

    constructor(goods: Goods, min: Float, max: Float, probability: Float) : this(
        goods = goods,
        amountMin = min,
        amountMax = max,
        amount = probability * (min + max) / 2,
        productivityAmount = probability * (min + max) / 2,
        probability = probability
    )

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
