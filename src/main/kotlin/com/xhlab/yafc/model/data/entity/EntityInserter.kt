package com.xhlab.yafc.model.data.entity

abstract class EntityInserter : Entity() {
    abstract val isStackInserter: Boolean
    abstract val inserterSwingTime: Float
}
