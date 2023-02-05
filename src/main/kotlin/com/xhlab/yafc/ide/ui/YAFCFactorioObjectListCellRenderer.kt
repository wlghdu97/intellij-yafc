package com.xhlab.yafc.ide.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.xhlab.yafc.model.data.FactorioObject
import javax.swing.JList

class YAFCFactorioObjectListCellRenderer(
    val project: Project,
    override val cellType: FactorioObjectCellType,
    private val extrasFragment: (FactorioObject) -> String? = { it.type }
) : ColoredListCellRenderer<FactorioObject>(), YAFCFactorioObjectCellRenderer {
    override val originalIPad = ipad

    override fun customizeCellRenderer(
        list: JList<out FactorioObject>,
        value: FactorioObject?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        this.customizeCellRenderer(project, value, extrasFragment)
    }
}
