package com.xhlab.yafc.ide.settings.yafc

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.DslConfigurableBase
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.ide.project.sync.YAFCSyncListener
import com.xhlab.yafc.model.YAFCProject

class YAFCPreferencesConfigurable(
    private val project: Project
) : DslConfigurableBase(), SearchableConfigurable, YAFCSyncListener, Disposable {
    private var view: YAFCPreferencesView? = null
    private val connection = project.messageBus.connect()

    init {
        connection.subscribe(YAFCProject.YAFC_SYNC_TOPIC, this)
    }

    override fun getId(): String = "settings.yafc.preferences"

    override fun getDisplayName(): String = YAFCBundle.message("settings.yafc.preferences.name")

    override fun createPanel(): DialogPanel {
        val view = YAFCPreferencesView(project)
        this.view = view
        return view.component
    }

    override fun isModified(): Boolean {
        if (project.service<YAFCProject>().storage == null) {
            return false
        }
        return super<DslConfigurableBase>.isModified()
    }

    override fun syncSucceeded(project: Project) {
        view?.reload(project)
    }

    override fun syncNeeded(project: Project) = Unit
    override fun syncStarted(project: Project) = Unit
    override fun syncFailed(project: Project) = Unit

    override fun dispose() {
        connection.disconnect()
    }
}
