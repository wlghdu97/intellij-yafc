package com.xhlab.yafc.ide.settings.factorio

import com.intellij.openapi.project.Project
import com.intellij.util.ui.FormBuilder
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathField
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathRef
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPathField
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPathRef
import javax.swing.JComponent
import javax.swing.JPanel

class FactorioSettingsView constructor(private val project: Project) {
    private val pathField = object : FactorioPathField(project) {
        override fun isDefaultProjectFactorioPathField() = true
    }
    private val modPathField = object : FactorioModPathField(project) {
        override fun isDefaultProjectFactorioModPathField() = true
    }
    val component: JComponent

    init {
        with(FormBuilder.createFormBuilder()) {
            setAlignLabelOnRight(false)
            addLabeledComponent(FactorioPathField.labelTextForComponent, pathField)
            addLabeledComponent(FactorioModPathField.labelTextForComponent, modPathField)
            addComponentFillVertically(JPanel(), 4)
            component = this.panel
        }
    }

    fun setFactorioPathRef(pathRef: FactorioPathRef) {
        pathField.setFactorioPathRef(pathRef)
    }

    fun getFactorioPathRef(): FactorioPathRef {
        return pathField.getFactorioPathRef()
    }

    fun setFactorioModPathRef(modPathRef: FactorioModPathRef) {
        modPathField.setFactorioModPathRef(modPathRef)
    }

    fun getFactorioModPathRef(): FactorioModPathRef {
        return modPathField.getFactorioModPathRef()
    }
}
