package com.xhlab.yafc.model

import com.xhlab.yafc.model.util.EnumFlag
import java.util.*

typealias ProjectPerItemFlags = EnumSet<ProjectPerItemFlag>

enum class ProjectPerItemFlag(override val value: Int) : EnumFlag {
    MILESTONE_UNLOCKED(1 shl 0),
    MARKED_ACCESSIBLE(1 shl 1),
    MARKED_INACCESSIBLE(1 shl 2);

    companion object {
        fun fromInt(value: Int): EnumSet<ProjectPerItemFlag> {
            val values = ProjectPerItemFlag.values().filter { it.value and value == it.value }
            return if (values.isEmpty()) {
                EnumSet.noneOf(ProjectPerItemFlag::class.java)
            } else {
                EnumSet.copyOf(values)
            }
        }
    }
}
