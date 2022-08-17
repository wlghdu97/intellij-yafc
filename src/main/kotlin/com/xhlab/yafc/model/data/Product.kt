package com.xhlab.yafc.model.data

sealed class Product : IFactorioObjectWrapper {
    abstract val goods: Goods
    abstract val amountMin: Float
    abstract val amountMax: Float
    abstract val productivityAmount: Float
    abstract val probability: Float

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

            return text
        }
}
