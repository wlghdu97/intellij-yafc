package com.xhlab.yafc.model.data

internal data class MutableProduct(
    override var goods: MutableGoods,
    override var amountMin: Float,
    override var amountMax: Float,
    override var amount: Float, // This is average amount including probability and range
    override var productivityAmount: Float,
    override var probability: Float
) : Product() {

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
}
