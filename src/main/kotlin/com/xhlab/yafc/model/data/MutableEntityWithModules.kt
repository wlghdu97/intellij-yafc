package com.xhlab.yafc.model.data

internal sealed interface MutableEntityWithModules : EntityWithModules, MutableEntity {
    override var allowedEffects: AllowedEffects
    override var moduleSlots: Int
}
