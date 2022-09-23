package com.xhlab.yafc.model.data

sealed interface EntityEnergy {
    val type: EntityEnergyType
    val workingTemperature: TemperatureRange
    val acceptedTemperature: TemperatureRange
    val emissions: Float
    val drain: Float
    val fuelConsumptionLimit: Float
    val fuels: List<Goods>
    val effectivity: Float
}
