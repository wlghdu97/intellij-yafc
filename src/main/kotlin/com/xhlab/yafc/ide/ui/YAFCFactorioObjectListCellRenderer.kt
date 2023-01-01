package com.xhlab.yafc.ide.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.RowIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import com.xhlab.yafc.model.YAFCProject
import com.xhlab.yafc.model.analysis.factorio.FactorioAnalysisType
import com.xhlab.yafc.model.analysis.factorio.FactorioMilestones
import com.xhlab.yafc.model.data.FactorioObject
import javax.swing.JList

class YAFCFactorioObjectListCellRenderer constructor(
    private val project: Project
) : ColoredListCellRenderer<FactorioObject>() {

    override fun customizeCellRenderer(
        list: JList<out FactorioObject>,
        value: FactorioObject,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        append(value.locName, SimpleTextAttributes.REGULAR_ATTRIBUTES, true)
        val storage = project.service<YAFCProject>().storage
        if (storage != null) {
            iconTextGap = 0
            icon = IconLoader.createLazy {
                val primary = IconCollection.getIcon(storage.dataSource, value) ?: IconUtil.getEmptyIcon(false)
                val milestones = storage.analyses.get<FactorioMilestones>(FactorioAnalysisType.MILESTONES)
                if (milestones != null) {
                    val secondary = milestones.getHighest(value, true)?.let {
                        IconCollection.getSmallIcon(storage.dataSource, it) ?: IconUtil.getEmptyIcon(false)
                    } ?: IconUtil.getEmptyIcon(false)
                    RowIcon(primary, secondary)
                } else {
                    primary
                }
            }
        }
    }
}
