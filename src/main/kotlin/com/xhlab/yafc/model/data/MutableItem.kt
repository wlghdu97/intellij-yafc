package com.xhlab.yafc.model.data

import javax.swing.Icon

internal data class MutableItem(
    override val name: String
) : Item(), MutableGoods {
    override var factorioType: String = ""
    override var originalName: String = ""
    override var typeDotName: String = ""
    override var locName: String = ""
    override var locDescr: String = ""
    override var iconSpec: List<FactorioIconPart> = emptyList()
    override var icon: Icon? = null
    override var id: FactorioId = FactorioId(-1)
    override var specialType: FactorioObjectSpecialType = FactorioObjectSpecialType.NORMAL
    override var fuelResult: MutableItem? = null
    override var stackSize: Int = 0
    override var placeResult: MutableEntity? = null
    override var module: ModuleSpecification? = null
    override var fuelValue: Float = 0f
    override var production: List<MutableRecipe> = emptyList()
    override var usages: List<MutableRecipe> = emptyList()
    override var miscSources: List<MutableFactorioObject> = emptyList()
    override var fuelFor: List<MutableEntity> = emptyList()
}
