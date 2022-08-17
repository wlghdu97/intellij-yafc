package com.xhlab.yafc.model.data

sealed class EntityBeacon : EntityWithModules {
    abstract val beaconEfficiency: Float
}
