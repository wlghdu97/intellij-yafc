package com.xhlab.yafc.model.data

import javax.swing.Icon

internal data class MutableEntityBeacon(
    override val name: String
) : EntityBeacon(), MutableEntityWithModules {
    override var factorioType: String = ""
    override var originalName: String = ""
    override var typeDotName: String = ""
    override var locName: String = ""
    override var locDescr: String = ""
    override var iconSpec: List<FactorioIconPart> = emptyList()
    override var icon: Icon? = null
    override var id: FactorioId = FactorioId(-1)
    override var specialType: FactorioObjectSpecialType = FactorioObjectSpecialType.NORMAL
    override var loot: List<MutableProduct> = emptyList()
    override var mapGenerated: Boolean = false
    override var mapGenDensity: Float = 0f
    override var power: Float = 0f
    override var energy: MutableEntityEnergy? = null
    override var itemsToPlace: List<MutableItem> = emptyList()
    override var size: Int = 0
    override var allowedEffects: AllowedEffects = AllowedEffects.NONE
    override var moduleSlots: Int = 0
    override var beaconEfficiency: Float = 0f
}
