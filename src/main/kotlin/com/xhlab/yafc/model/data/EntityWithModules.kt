package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.util.and
import com.xhlab.yafc.model.util.or
import java.util.*

sealed interface EntityWithModules : Entity {
    val allowedEffects: AllowedEffects
    val moduleSlots: Int

    fun canAcceptModule(module: ModuleSpecification) = canAcceptModule(module, allowedEffects)

    companion object {
        fun canAcceptModule(module: ModuleSpecification, effects: AllowedEffects): Boolean {
            return when {
                (effects == EnumSet.allOf(AllowedEffect::class.java)) -> {
                    true
                }

                (effects == (AllowedEffect.CONSUMPTION or AllowedEffect.POLLUTION or AllowedEffect.SPEED)) -> {
                    module.productivity == 0f
                }

                (effects == EnumSet.noneOf(AllowedEffect::class.java)) -> {
                    false
                }

                // check the rest
                (module.productivity != 0f && (effects and AllowedEffect.PRODUCTIVITY).isEmpty()) -> {
                    false
                }

                (module.consumption != 0f && (effects and AllowedEffect.CONSUMPTION).isEmpty()) -> {
                    false
                }

                (module.pollution != 0f && (effects and AllowedEffect.POLLUTION).isEmpty()) -> {
                    false
                }

                (module.speed != 0f && (effects and AllowedEffect.SPEED).isEmpty()) -> {
                    false
                }

                else -> {
                    true
                }
            }
        }
    }
}
