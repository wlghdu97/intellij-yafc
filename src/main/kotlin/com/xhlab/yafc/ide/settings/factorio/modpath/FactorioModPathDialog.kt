package com.xhlab.yafc.ide.settings.factorio.modpath

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders
import com.intellij.util.ui.update.UiNotifyConnector
import com.xhlab.yafc.ide.YAFCBundle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
import javax.swing.ScrollPaneConstants

class FactorioModPathDialog constructor(
    private val project: Project,
    private val projectDefaultField: Boolean
) : DialogWrapper(project, true) {
    private val listModel = DefaultListModel<FactorioModPathRef>()
    private val list = JBList(listModel)
    private val centerPaneComponent: JComponent
    private val changesMap = hashMapOf<FactorioModPathRef, FactorioModPath>()

    private val prevProjectFactorioModPathRef: FactorioModPathRef? = if (projectDefaultField) {
        null
    } else {
        service<FactorioModPathManager>().getProjectFactorioModPathRef(project)
    }

    init {
        with(list) {
            selectionMode = MULTIPLE_INTERVAL_SELECTION
            cellRenderer = FactorioModPathRenderer(project)
            setEmptyText(this@FactorioModPathDialog.getEmptyText())
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (e.isPopupTrigger) {
                        val row: Int = list.locationToIndex(e.point)
                        if (row >= 0) {
                            list.selectedIndex = row
                        }
                    }
                }
            })
        }

        val decorator = ToolbarDecorator.createDecorator(list)
            .setPanelBorder(Borders.empty())
            .setAddAction {
                performAddAction()
            }
            .setRemoveAction {
                ListUtil.removeSelectedItems(list) { it != null }
            }
            .setRemoveActionUpdater {
                list.selectedValue != null
            }
            .setEditAction {
                performEditAction()
            }
            .setEditActionUpdater {
                isEditAvailable()
            }
            .disableUpDownActions()
        centerPaneComponent = decorator.createPanel().apply {
            object : DoubleClickListener() {
                override fun onDoubleClick(event: MouseEvent): Boolean {
                    return if (isEditAvailable()) {
                        performEditAction()
                        true
                    } else {
                        false
                    }
                }
            }.installOn(this)
            preferredSize = JBUI.size(550, 300)
        }

        val pane = ComponentUtil.getScrollPane(list)
        if (pane != null) {
            pane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        title = YAFCBundle.message("yafc.factorio.mod.paths")
        init()
        configurePopupHandler()
        fillList()
    }

    private fun configurePopupHandler() {
        val actionList = arrayListOf<AnAction>()
        if (!projectDefaultField) {
            actionList.add(SetAsProjectDefaultFactorioModPathAction())
        }

        val actionGroup = DefaultActionGroup(actionList)
        actionList.forEach {
            it.registerCustomShortcutSet(it.shortcutSet, list)
        }

        PopupHandler.installPopupMenu(list, actionGroup, "YAFCFactorioModPathListPopup")
    }

    private fun getSingleSelectedFactorioModPathRef(): FactorioModPathRef? {
        return if (list.minSelectionIndex != list.maxSelectionIndex) {
            null
        } else {
            list.selectedValue
        }
    }

    private fun isEditAvailable(): Boolean {
        return getSingleSelectedFactorioModPathRef() != null
    }

    private fun performEditAction() {
        var selectedIdx = list.selectedIndex
        if (selectedIdx >= 0) {
            val modPathRef = listModel.elementAt(selectedIdx)
            if (modPathRef != null) {
                val factorioModPath = modPathRef.resolve(project) ?: return
                val newFactorioModPath = chooseFactorioModPath(factorioModPath)
                if (newFactorioModPath != null) {
                    val ref = newFactorioModPath.toRef()
                    val index = listModel.indexOf(ref)
                    if (index != -1 && index != selectedIdx) {
                        list.selectedIndex = index
                        selectedIdx = index
                    }

                    listModel.set(selectedIdx, ref)
                    changesMap[ref] = newFactorioModPath
                }
            }
        }
    }

    private fun fillList() {
        service<FactorioModPathManager>().getFactorioModPaths().forEach {
            listModel.addElement(it.toRef())
        }
    }

    private fun getEmptyText(): String {
        val shortcutSet = CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD)
        val shortcut = ArrayUtil.getFirstElement(shortcutSet.shortcuts)
        return if (shortcut != null) {
            YAFCBundle.message(
                "status.text.add.factorio.mod.path.with",
                KeymapUtil.getShortcutText(shortcut)
            )
        } else {
            YAFCBundle.message("status.text.no.factorio.mod.paths.added")
        }
    }

    override fun getStyle(): DialogStyle {
        return DialogStyle.COMPACT
    }

    override fun getDimensionServiceKey(): String {
        return DIMENSION_SERVICE_KEY
    }

    override fun createCenterPanel(): JComponent {
        return centerPaneComponent
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return list
    }

    private fun getFactorioModPathRefs(): List<FactorioModPathRef> {
        val paths = arrayListOf<FactorioModPathRef>()
        for (index in 0 until listModel.size()) {
            val ref = listModel.elementAt(index)
            if (ref != null) {
                paths.add(ref)
            }
        }
        return paths
    }

    private fun performAddAction() {
        val newFactorioModPath = chooseFactorioModPath()
        if (newFactorioModPath != null) {
            val ref = newFactorioModPath.toRef()
            changesMap[ref] = newFactorioModPath
            addIfNeededAndSelect(ref)
        }
    }

    private fun chooseFactorioModPath(factorioModPath: FactorioModPath? = null): FactorioModPath? {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        if (SystemInfo.isMac) {
            descriptor.isForcedToUseIdeaFileChooser = true
        }

        val initial = if (factorioModPath != null) {
            LocalFileSystem.getInstance().findFileByPath(factorioModPath.systemIndependentPath)
        } else {
            null
        }

        return FileChooser.chooseFile(descriptor, project, initial)?.let {
            FactorioModPath(it.path)
        }
    }

    fun showAndGetSelected(initialSelection: FactorioModPathRef?): Ref<FactorioModPathRef>? {
        if (initialSelection != null && initialSelection != FactorioModPathField.NO_FACTORIO_MOD_PATH_REF) {
            addIfNeededAndSelect(initialSelection)
        } else {
            list.clearSelection()
        }

        val modPathManager = service<FactorioModPathManager>()
        if (!showAndGet()) {
            if (prevProjectFactorioModPathRef != null) {
                modPathManager.setProjectFactorioModPath(project, prevProjectFactorioModPathRef)
            }

            return null
        } else {
            val modifiedPaths = arrayListOf<FactorioModPath>()
            getFactorioModPathRefs().forEach {
                val path = changesMap[it] ?: it.resolve(project)
                if (path != null) {
                    modifiedPaths.add(path)
                }
            }
            modPathManager.setFactorioModPaths(modifiedPaths)

            return Ref.create(list.selectedValue as FactorioModPathRef)
        }
    }

    private fun addIfNeededAndSelect(modPathRef: FactorioModPathRef) {
        var index = listModel.indexOf(modPathRef)
        if (index == -1) {
            listModel.addElement(modPathRef)
            index = listModel.size() - 1
        }
        list.selectedIndex = index
        UiNotifyConnector.doWhenFirstShown(list) {
            ScrollingUtil.ensureIndexIsVisible(list, index, 1)
        }
    }

    private inner class SetAsProjectDefaultFactorioModPathAction :
        DumbAwareAction(YAFCBundle.message("yafc.set.project.factorio.mod.path.action")) {

        override fun update(e: AnActionEvent) {
            val ref = getSingleSelectedFactorioModPathRef()
            e.presentation.isEnabledAndVisible = canUseAsProjectDefaultFactorioModPathRef(ref)
        }

        private fun canUseAsProjectDefaultFactorioModPathRef(modPathRef: FactorioModPathRef?): Boolean {
            return (modPathRef != null && !modPathRef.isProjectRef())
        }

        override fun actionPerformed(e: AnActionEvent) {
            val ref = getSingleSelectedFactorioModPathRef()
            if (canUseAsProjectDefaultFactorioModPathRef(ref)) {
                service<FactorioModPathManager>().setProjectFactorioModPath(project, ref)
                list.repaint()
            }
        }
    }

    companion object {
        private const val DIMENSION_SERVICE_KEY = "yafc.factorio-mod-paths-dialog"
    }
}
