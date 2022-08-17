package com.xhlab.yafc.model.data

sealed class EntityReactor : EntityCrafter {
    abstract val reactorNeighbourBonus: Float
}
