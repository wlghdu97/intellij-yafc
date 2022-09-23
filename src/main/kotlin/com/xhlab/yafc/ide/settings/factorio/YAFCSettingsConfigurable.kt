package com.xhlab.yafc.ide.settings.factorio

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathManager
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPathManager
import com.xhlab.yafc.model.YAFCProject
import javax.swing.JComponent

class YAFCSettingsConfigurable constructor(
    private val project: Project
) : SearchableConfigurable, NoScroll {
    private var view: FactorioSettingsView? = null

    override fun getId(): String = "settings.yafc"

    override fun getDisplayName(): String = YAFCBundle.message("settings.yafc.name")

    override fun createComponent(): JComponent {
        val view = FactorioSettingsView(project)
        this.view = view
        return view.component
    }

    override fun isModified(): Boolean {
        val view = view
        return if (view == null) {
            false
        } else {
            (isApplicationPathModified(view) || isModPathModified(view))
        }
    }

    private fun isApplicationPathModified(view: FactorioSettingsView): Boolean {
        val oldPathRef = service<FactorioPathManager>().getProjectFactorioPathRef(project)
        val newPathRef = view.getFactorioPathRef()
        return oldPathRef != newPathRef
    }

    private fun isModPathModified(view: FactorioSettingsView): Boolean {
        val oldModPathRef = service<FactorioModPathManager>().getProjectFactorioModPathRef(project)
        val newModPathRef = view.getFactorioModPathRef()
        return oldModPathRef != newModPathRef
    }

    override fun apply() {
        val view = view
        if (view != null) {
            val factorioPathManager = service<FactorioPathManager>()
            val oldPathRef = factorioPathManager.getProjectFactorioPathRef(project)
            val newPathRef = view.getFactorioPathRef()
            factorioPathManager.setProjectFactorioPath(project, newPathRef)

            val factorioModPathManager = service<FactorioModPathManager>()
            val oldModPathRef = factorioModPathManager.getProjectFactorioModPathRef(project)
            val newModPathRef = view.getFactorioModPathRef()
            factorioModPathManager.setProjectFactorioModPath(project, newModPathRef)

            if (oldPathRef != newPathRef || oldModPathRef != newModPathRef) {
                project.service<YAFCProject>().syncDatabase()
            }
        }
    }

    override fun reset() {
        view?.let {
            it.setFactorioPathRef(service<FactorioPathManager>().getProjectFactorioPathRef(project))
            it.setFactorioModPathRef(service<FactorioModPathManager>().getProjectFactorioModPathRef(project))
        }
    }
}
