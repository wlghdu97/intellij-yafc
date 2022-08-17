package com.xhlab.yafc.model.data

internal sealed interface MutableGoods : Goods, MutableFactorioObject {
    override var fuelValue: Float
    override val isPower: Boolean
    override var production: List<MutableRecipe>
    override var usages: List<MutableRecipe>
    override var miscSources: List<MutableFactorioObject>
    override var fuelFor: List<MutableEntity>
    override val flowUnitOfMeasure: UnitOfMeasure
}
