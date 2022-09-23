package com.xhlab.yafc.model.data

import javax.swing.Icon

internal data class MutableFluid(
    override var name: String
) : Fluid(), MutableGoods {
    override var factorioType: String = ""
    override var originalName: String = ""
    override var typeDotName: String = ""
    override var locName: String = ""
    override var locDescr: String = ""
    override var iconSpec: List<FactorioIconPart> = emptyList()
    override var icon: Icon? = null
    override var id: FactorioId = FactorioId(-1)
    override var specialType: FactorioObjectSpecialType = FactorioObjectSpecialType.NORMAL
    override var heatCapacity: Float = 1e-3f
    override var temperatureRange: TemperatureRange = TemperatureRange.Any
    override var temperature: Int = 0
    override var heatValue: Float = 0f
    override var fuelValue: Float = 0f
    override var production: List<MutableRecipe> = emptyList()
    override var usages: List<MutableRecipe> = emptyList()
    override var miscSources: List<MutableFactorioObject> = emptyList()
    override var fuelFor: List<MutableEntity> = emptyList()
    override var variants: ArrayList<MutableFluid> = arrayListOf()

    internal fun setTemperature(temp: Int) {
        temperature = temp
        heatValue = (temp - temperatureRange.min) * heatCapacity
    }
}

