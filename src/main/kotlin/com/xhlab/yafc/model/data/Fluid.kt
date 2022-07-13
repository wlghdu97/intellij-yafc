package com.xhlab.yafc.model.data

abstract class Fluid : Goods() {
    open val heatCapacity: Float = 1e-3f
    abstract val temperatureRange: TemperatureRange
    abstract var temperature: Int
    abstract var heatValue: Float
    abstract val variants: List<Fluid>

    override val isPower: Boolean = false
    override val type: String = "Fluid"
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.FLUIDS
    override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.FLUID_PER_SECOND

    internal fun setTemperature(temp: Int) {
        temperature = temp
        heatValue = (temp - temperatureRange.min) * heatCapacity
    }
}
