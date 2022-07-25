package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.data.entity.Entity
import javax.swing.Icon

internal data class MutableFluid(
    override val name: String,
    override var factorioType: String? = null,
    override var originalName: String? = null,
    override var typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override var icon: Icon? = null,
    override var id: FactorioId? = null,
    var heatCapacity: Float = 1e-3f,
    var temperatureRange: TemperatureRange = TemperatureRange.Any,
    var temperature: Int = 0,
    var heatValue: Float = 0f,
    override var fuelValue: Float = 0f,
    override var production: List<Recipe> = emptyList(),
    override var usages: List<Recipe> = emptyList(),
    override var miscSources: List<FactorioObject> = emptyList(),
    override var fuelFor: List<Entity> = emptyList(),
) : MutableGoods(name) {
    var variants: MutableList<MutableFluid>? = null

    override val isPower: Boolean = false
    override val type: String = "Fluid"
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.FLUIDS
    override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.FLUID_PER_SECOND

    internal fun setTemperature(temp: Int) {
        temperature = temp
        heatValue = (temp - temperatureRange.min) * heatCapacity
    }
}

