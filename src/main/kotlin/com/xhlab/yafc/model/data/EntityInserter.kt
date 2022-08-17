package com.xhlab.yafc.model.data

sealed class EntityInserter : Entity {
    abstract val isStackInserter: Boolean
    abstract val inserterSwingTime: Float
}
