package com.xhlab.yafc.ide.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
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
    }
}
