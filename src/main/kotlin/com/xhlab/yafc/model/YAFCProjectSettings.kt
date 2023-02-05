package com.xhlab.yafc.model

import java.util.*

interface YAFCProjectSettings {
    val milestones: List<String>
    val itemFlags: SortedMap<String, ProjectPerItemFlags>
    val miningProductivity: Float
    var reactorSizeX: Float
    var reactorSizeY: Float

    val reactorBonusMultiplier: Float
        get() = 4f - 2f / reactorSizeX - 2f / reactorSizeY

    fun setMilestones(newMilestones: List<Pair<String, Boolean>>)
    fun setFlag(itemKey: String, flag: ProjectPerItemFlags, set: Boolean)

    fun flags(typeDotName: String): ProjectPerItemFlags {
        return itemFlags[typeDotName] ?: EnumSet.noneOf(ProjectPerItemFlag::class.java)
    }
}
