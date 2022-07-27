package com.xhlab.yafc.parser.data.mutable.entity

import com.xhlab.yafc.model.data.Goods
import com.xhlab.yafc.model.data.TemperatureRange
import com.xhlab.yafc.model.data.entity.EntityEnergyType

data class MutableEntityEnergy(
    var type: EntityEnergyType? = null,
    var workingTemperature: TemperatureRange = TemperatureRange.Any,
    var acceptedTemperature: TemperatureRange = TemperatureRange.Any,
    var emissions: Float = 0f,
    var drain: Float = 0f,
    var fuelConsumptionLimit: Float = Float.POSITIVE_INFINITY,
    var fuels: List<Goods> = emptyList(),
    var effectivity: Float = 1f
)
