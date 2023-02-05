package com.xhlab.yafc.model.data

sealed class Fluid : Goods {
    abstract val heatCapacity: Float
    abstract val temperatureRange: TemperatureRange
    abstract val temperature: Int
    abstract val heatValue: Float
    abstract val variants: List<Fluid>

    final override val isPower: Boolean = false
    final override val type: String = "Fluid"
    final override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.FLUIDS
    final override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.Preference.FLUID_PER_SECOND
}
