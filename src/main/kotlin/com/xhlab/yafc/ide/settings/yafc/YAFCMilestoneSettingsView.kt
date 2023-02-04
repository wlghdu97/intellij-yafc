package com.xhlab.yafc.ide.settings.yafc

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.TitledSeparator
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.table.TableView
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.update.UiNotifyConnector
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.ide.ui.YAFCObjectChooserDialog
import com.xhlab.yafc.model.YAFCProject
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * @see com.intellij.util.ui.tree.PerFileConfigurableBase
 */
class YAFCMilestoneSettingsView constructor(project: Project) {
    private val headerPanel = JPanel(VerticalLayout(0)).apply {
        add(TitledSeparator(YAFCBundle.message("settings.yafc.milestones.title")))
        add(JBLabel(YAFCBundle.message("settings.yafc.milestones.header")).apply {
            border = IdeBorderFactory.createEmptyBorder(JBUI.insetsBottom(8))
            foreground = UIUtil.getContextHelpForeground()
        })
    }

    val tableModel = YAFCMilestoneModel.createModel(project)

    private val table = TableView(tableModel).apply {
        emptyText.text = YAFCBundle.message("settings.yafc.milestones.empty.or.syncing.not.finished")
    }

    private val panelMilestoneListChooser = with(table) {
        ToolbarDecorator.createDecorator(this)
            .setAddActionUpdater {
                tableModel.items.size < 60
            }
            .setAddAction {
                val milestone = YAFCBundle.message("yafc.milestone")
                val db = project.service<YAFCProject>().storage?.db ?: return@setAddAction
                val target = db.objects.all
                val excluded = tableModel.items.map { it.target }.toSet()
                val dialog = YAFCObjectChooserDialog(project, milestone, target, excluded)
                if (dialog.showAndGet()) {
                    if (dialog.selectedObjects.isNotEmpty()) {
                        val newDescriptor = YAFCMilestoneDescriptor(dialog.selectedObjects[0], false)
                        tableModel.addRow(newDescriptor)
                        selectIfNeeded(newDescriptor)
                    }
                }
            }
            .createPanel()
    }

    private val infoPanel = JPanel(VerticalLayout(4, SwingConstants.LEFT)).apply {
        border = IdeBorderFactory.createEmptyBorder(JBUI.insetsTop(8))
        add(JBLabel(YAFCBundle.message("settings.yafc.milestones.description1")).apply {
            componentStyle = UIUtil.ComponentStyle.SMALL
        })
        add(JBLabel(YAFCBundle.message("settings.yafc.milestones.description2")).apply {
            componentStyle = UIUtil.ComponentStyle.SMALL
        })
    }

    private val root = JPanel(BorderLayout()).apply {
        add(headerPanel, BorderLayout.NORTH)
        add(panelMilestoneListChooser, BorderLayout.CENTER)
        add(infoPanel, BorderLayout.SOUTH)
    }

    val component: JComponent = root

    private fun selectIfNeeded(descriptor: YAFCMilestoneDescriptor) {
        table.selection = listOf(descriptor)
        val index = tableModel.indexOf(descriptor)
        if (index != -1) {
            UiNotifyConnector.doWhenFirstShown(table) {
                ScrollingUtil.ensureIndexIsVisible(table, index, 1)
            }
        }
    }
}
