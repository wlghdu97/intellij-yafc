package com.xhlab.yafc.parser.data.mutable.entity

import com.xhlab.yafc.model.data.AllowedEffects

internal abstract class MutableEntityWithModules : MutableEntity() {
    open var allowedEffects: AllowedEffects = AllowedEffects.NONE
    abstract var moduleSlots: Int
}
