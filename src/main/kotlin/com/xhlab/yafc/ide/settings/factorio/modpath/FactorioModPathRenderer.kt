package com.xhlab.yafc.ide.settings.factorio.modpath

import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.xhlab.yafc.ide.YAFCBundle
import javax.swing.JList

class FactorioModPathRenderer constructor(
    private val project: Project
) : ColoredListCellRenderer<FactorioModPathRef>() {

    override fun customizeCellRenderer(
        list: JList<out FactorioModPathRef>,
        value: FactorioModPathRef,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        if (value == FactorioModPathField.NO_FACTORIO_MOD_PATH_REF) {
            append(
                YAFCBundle.message("yafc.no.factorio.mod.path"),
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            )
        } else {
            val path = value.resolve(project)
            val errorMessage = path?.validate()
            toolTipText = errorMessage

            if (path != null) {
                val valid = (errorMessage == null)
                val attributes = if (valid) {
                    SimpleTextAttributes.REGULAR_ATTRIBUTES
                } else {
                    SimpleTextAttributes.ERROR_ATTRIBUTES
                }
                append(path.presentableName, attributes)
            } else {
                append(
                    "${value.referenceName} (${YAFCBundle.message("yafc.no.factorio.mod.path.reference.found")})",
                    SimpleTextAttributes.ERROR_ATTRIBUTES
                )
            }
        }
    }
}
