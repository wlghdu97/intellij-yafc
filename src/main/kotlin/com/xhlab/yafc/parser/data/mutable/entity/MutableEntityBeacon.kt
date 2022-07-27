package com.xhlab.yafc.parser.data.mutable.entity

import com.xhlab.yafc.model.data.AllowedEffects
import com.xhlab.yafc.model.data.FactorioIconPart
import com.xhlab.yafc.model.data.FactorioId
import com.xhlab.yafc.parser.data.mutable.MutableItem
import com.xhlab.yafc.parser.data.mutable.MutableProduct
import javax.swing.Icon

internal data class MutableEntityBeacon(
    override val name: String,
    override var factorioType: String? = null,
    override var originalName: String? = null,
    override var typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override var icon: Icon? = null,
    override var id: FactorioId? = null,
    override var loot: List<MutableProduct> = emptyList(),
    override var mapGenerated: Boolean = false,
    override var mapGenDensity: Float = 0f,
    override var power: Float = 0f,
    override var energy: MutableEntityEnergy? = null,
    override var itemToPlace: List<MutableItem> = emptyList(),
    override var size: Int = 0,
    override var allowedEffects: AllowedEffects = AllowedEffects.NONE,
    override var moduleSlots: Int = 0,
    var beaconEfficiency: Float = 0f
) : MutableEntityWithModules()
