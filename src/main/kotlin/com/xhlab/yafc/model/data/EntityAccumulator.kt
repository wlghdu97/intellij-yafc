package com.xhlab.yafc.model.data

sealed class EntityAccumulator : Entity {
    abstract val accumulatorCapacity: Float
}
