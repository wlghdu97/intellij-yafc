package com.xhlab.yafc.model.data.entity

import com.xhlab.yafc.model.data.AllowedEffects
import com.xhlab.yafc.model.data.ModuleSpecification

abstract class EntityWithModules : Entity() {

    open val allowedEffects: AllowedEffects = AllowedEffects.NONE
    abstract val moduleSlots: Int

    fun canAcceptModule(module: ModuleSpecification) = canAcceptModule(module, allowedEffects)

    companion object {
        fun canAcceptModule(module: ModuleSpecification, effects: AllowedEffects): Boolean {
            return when {
                (effects == AllowedEffects.ALL) -> {
                    true
                }

                (effects.value == (AllowedEffects.CONSUMPTION.value or AllowedEffects.POLLUTION.value or AllowedEffects.SPEED.value)) -> {
                    module.productivity == 0f
                }

                (effects == AllowedEffects.NONE) -> {
                    false
                }

                // check the rest
                (module.productivity != 0f && (effects.value and AllowedEffects.PRODUCTIVITY.value) == 0) -> {
                    false
                }

                (module.consumption != 0f && (effects.value and AllowedEffects.CONSUMPTION.value) == 0) -> {
                    false
                }

                (module.pollution != 0f && (effects.value and AllowedEffects.POLLUTION.value) == 0) -> {
                    false
                }

                (module.speed != 0f && (effects.value and AllowedEffects.SPEED.value) == 0) -> {
                    false
                }

                else -> {
                    true
                }
            }
        }
    }
}
