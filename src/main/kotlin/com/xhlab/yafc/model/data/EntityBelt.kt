package com.xhlab.yafc.model.data

sealed class EntityBelt : Entity {
    abstract val beltItemsPerSecond: Float
}
