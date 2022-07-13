package com.xhlab.yafc.model.data.entity

import com.xhlab.yafc.model.data.Goods
import com.xhlab.yafc.model.data.TemperatureRange

data class EntityEnergy(
    val type: EntityEnergyType,
    val workingTemperature: TemperatureRange = TemperatureRange.Any,
    val acceptedTemperature: TemperatureRange = TemperatureRange.Any,
    val emissions: Float = 0f,
    val drain: Float = 0f,
    val fuelConsumptionLimit: Float = Float.POSITIVE_INFINITY,
    val fuels: List<Goods> = emptyList(),
    val effectivity: Float = 1f
)
