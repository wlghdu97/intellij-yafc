package com.xhlab.yafc.model.data

internal data class MutableEntityEnergy(
    override var type: EntityEnergyType = EntityEnergyType.NONE,
    override var workingTemperature: TemperatureRange = TemperatureRange.Any,
    override var acceptedTemperature: TemperatureRange = TemperatureRange.Any,
    override var emissions: Float = 0f,
    override var drain: Float = 0f,
    override var fuelConsumptionLimit: Float = Float.POSITIVE_INFINITY,
    override var fuels: List<MutableGoods> = emptyList(),
    override var effectivity: Float = 1f
) : EntityEnergy
