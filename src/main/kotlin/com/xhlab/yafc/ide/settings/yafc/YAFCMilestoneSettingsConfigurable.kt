package com.xhlab.yafc.ide.settings.yafc

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.ide.project.sync.YAFCSyncListener
import com.xhlab.yafc.model.ProjectPerItemFlag
import com.xhlab.yafc.model.YAFCProject
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.analysis.factorio.FactorioAnalysisType
import com.xhlab.yafc.model.data.DataUtils
import java.util.*
import javax.swing.JComponent

class YAFCMilestoneSettingsConfigurable constructor(
    private val project: Project
) : SearchableConfigurable, YAFCSyncListener, Disposable {
    private var view: YAFCMilestoneSettingsView? = null
    private val connection = project.messageBus.connect()

    init {
        connection.subscribe(YAFCProject.YAFC_SYNC_TOPIC, this)
    }

    override fun getId(): String = "settings.yafc.milestones"

    override fun getDisplayName(): String = YAFCBundle.message("settings.yafc.milestones.name")

    override fun createComponent(): JComponent {
        val view = YAFCMilestoneSettingsView(project)
        this.view = view
        return view.component
    }

    override fun isModified(): Boolean {
        if (project.service<YAFCProject>().storage == null) {
            return false
        }
        return view?.tableModel?.isModified() ?: false
    }

    private fun YAFCMilestoneModel.isModified(): Boolean {
        val settingsModel = project.service<YAFCProjectSettings>()
        if (settingsModel.milestones != items.map { it.target.typeDotName }) {
            return true
        }
        val unlockedMilestoneKeysInSettings = settingsModel.itemFlags.filterValues {
            DataUtils.hasFlags(it, EnumSet.of(ProjectPerItemFlag.MILESTONE_UNLOCKED))
        }.keys
        val unlockedMilestoneKeys = items.asSequence().filter { it.isEnabled }.map { it.target.typeDotName }.toSet()
        if (unlockedMilestoneKeysInSettings != unlockedMilestoneKeys) {
            return true
        }
        return false
    }

    override fun apply() {
        val view = view
        if (view != null) {
            project.service<YAFCProjectSettings>()
                .setMilestones(view.tableModel.items.map { it.target.typeDotName to it.isEnabled })
            // compute only analyses
            project.service<YAFCProject>().recomputeAnalysis(FactorioAnalysisType.MILESTONES)
        }
    }

    override fun reset() {
        view?.tableModel?.items = YAFCMilestoneModel.createModelItems(project)
    }

    override fun syncSucceeded(project: Project) {
        reset()
    }

    override fun syncNeeded(project: Project) = Unit
    override fun syncStarted(project: Project) = Unit
    override fun syncFailed(project: Project) = Unit

    override fun dispose() {
        connection.disconnect()
    }
}
