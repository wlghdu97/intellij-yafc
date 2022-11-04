package com.xhlab.yafc.model.data

import javax.swing.Icon

internal data class MutableSpecial(
    override val name: String,
    override val virtualSignal: String,
    internal val power: Boolean
) : Special(), MutableGoods {
    override var factorioType: String = ""
    override var originalName: String = ""
    override var locName: String = ""
    override var locDescr: String = ""
    override var iconSpec: List<FactorioIconPart> = emptyList()
    override var icon: Icon? = null
    override var id: FactorioId = FactorioId(-1)
    override var specialType: FactorioObjectSpecialType = FactorioObjectSpecialType.NORMAL
    override var fuelValue: Float = 0f
    override var production: List<MutableRecipe> = emptyList()
    override var usages: List<MutableRecipe> = emptyList()
    override var miscSources: List<MutableFactorioObject> = emptyList()
    override var fuelFor: List<MutableEntity> = emptyList()
    override val isPower: Boolean = power
}
