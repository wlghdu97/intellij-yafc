package com.xhlab.yafc.model.data.entity

abstract class EntityContainer : Entity() {
    abstract val inventorySize: Int
    abstract val logisticMode: String
    abstract val logisticSlotCount: Int
}
