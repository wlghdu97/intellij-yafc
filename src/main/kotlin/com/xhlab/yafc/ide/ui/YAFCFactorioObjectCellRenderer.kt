package com.xhlab.yafc.ide.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.xhlab.yafc.model.YAFCProject
import com.xhlab.yafc.model.data.FactorioObject
import java.awt.Insets

interface YAFCFactorioObjectCellRenderer {
    val cellType: FactorioObjectCellType
    val originalIPad: Insets

    fun SimpleColoredComponent.customizeCellRenderer(
        project: Project,
        value: FactorioObject?,
        extraFragment: (FactorioObject) -> String? = { null }
    ) {
        if (value != null) {
            append(value.locName, SimpleTextAttributes.REGULAR_ATTRIBUTES, true)
            val extra = extraFragment(value)
            if (extra != null) {
                append(" $extra", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
            }

            val storage = project.service<YAFCProject>().storage
            if (storage != null) {
                when (cellType) {
                    FactorioObjectCellType.NORMAL -> {
                        iconTextGap = 0
                        ipad = originalIPad
                    }

                    FactorioObjectCellType.BIG -> {
                        iconTextGap = 8
                        ipad = originalIPad.apply {
                            top = 4
                            bottom = 4
                        }
                    }
                }
                icon = IconLoader.createLazy {
                    IconCollection.getIconWithHighestMilestoneBadge(storage, value, cellType)
                }
            }
        }
    }
}
