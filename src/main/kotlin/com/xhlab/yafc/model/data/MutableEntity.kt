package com.xhlab.yafc.model.data

internal sealed interface MutableEntity : Entity, MutableFactorioObject {
    override var loot: List<MutableProduct>
    override var mapGenerated: Boolean
    override var mapGenDensity: Float
    override var power: Float
    override var energy: MutableEntityEnergy?
    override var itemsToPlace: List<MutableItem>
    override var size: Int
}
