package com.xhlab.yafc.model.data

sealed class EntityContainer : Entity {
    abstract val inventorySize: Int
    abstract val logisticMode: String?
    abstract val logisticSlotsCount: Int
}
