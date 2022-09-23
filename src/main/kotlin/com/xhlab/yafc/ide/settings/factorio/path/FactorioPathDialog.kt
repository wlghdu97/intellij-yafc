package com.xhlab.yafc.ide.settings.factorio.path

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
import com.intellij.ui.CommonActionsPanel.Buttons
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

class FactorioPathDialog constructor(
    private val project: Project,
    private val projectDefaultField: Boolean
) : DialogWrapper(project, true) {
    private val listModel = DefaultListModel<FactorioPathRef>()
    private val list = JBList(listModel)
    private val centerPaneComponent: JComponent
    private val changesMap = hashMapOf<FactorioPathRef, FactorioPath>()

    private val prevProjectFactorioPathRef: FactorioPathRef? = if (projectDefaultField) {
        null
    } else {
        service<FactorioPathManager>().getProjectFactorioPathRef(project)
    }

    init {
        with(list) {
            selectionMode = MULTIPLE_INTERVAL_SELECTION
            cellRenderer = FactorioPathRenderer(project, false)
            setEmptyText(this@FactorioPathDialog.getEmptyText())
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

        title = YAFCBundle.message("yafc.factorio.paths")
        init()
        configurePopupHandler()
        fillList()
    }

    private fun configurePopupHandler() {
        val actionList = arrayListOf<AnAction>()
        if (!projectDefaultField) {
            actionList.add(SetAsProjectDefaultFactorioPathAction())
        }

        val actionGroup = DefaultActionGroup(actionList)
        actionList.forEach {
            it.registerCustomShortcutSet(it.shortcutSet, list)
        }

        PopupHandler.installPopupMenu(list, actionGroup, "YAFCFactorioPathListPopup")
    }

    private fun getSingleSelectedFactorioPathRef(): FactorioPathRef? {
        return if (list.minSelectionIndex != list.maxSelectionIndex) {
            null
        } else {
            list.selectedValue
        }
    }

    private fun isEditAvailable(): Boolean {
        return getSingleSelectedFactorioPathRef() != null
    }

    private fun performEditAction() {
        var selectedIdx = list.selectedIndex
        if (selectedIdx >= 0) {
            val pathRef = listModel.elementAt(selectedIdx)
            if (pathRef != null) {
                val factorioPath = pathRef.resolve(project) ?: return
                val newFactorioPath = chooseFactorioPath(factorioPath)
                if (newFactorioPath != null) {
                    val ref = newFactorioPath.toRef()
                    val index = listModel.indexOf(ref)
                    if (index != -1 && index != selectedIdx) {
                        list.selectedIndex = index
                        selectedIdx = index
                    }

                    listModel.set(selectedIdx, ref)
                    changesMap[ref] = newFactorioPath
                }
            }
        }
    }

    private fun fillList() {
        service<FactorioPathManager>().getFactorioPaths().forEach {
            listModel.addElement(it.toRef())
        }
    }

    private fun getEmptyText(): String {
        val shortcutSet = CommonActionsPanel.getCommonShortcut(Buttons.ADD)
        val shortcut = ArrayUtil.getFirstElement(shortcutSet.shortcuts)
        return if (shortcut != null) {
            YAFCBundle.message(
                "status.text.add.factorio.path.with",
                KeymapUtil.getShortcutText(shortcut)
            )
        } else {
            YAFCBundle.message("status.text.no.factorio.paths.added")
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

    private fun getFactorioPathRefs(): List<FactorioPathRef> {
        val paths = arrayListOf<FactorioPathRef>()
        for (index in 0 until listModel.size()) {
            val ref = listModel.elementAt(index)
            if (ref != null) {
                paths.add(ref)
            }
        }
        return paths
    }

    private fun performAddAction() {
        val newFactorioPath = chooseFactorioPath()
        if (newFactorioPath != null) {
            val ref = newFactorioPath.toRef()
            changesMap[ref] = newFactorioPath
            addIfNeededAndSelect(ref)
        }
    }

    private fun chooseFactorioPath(factorioPath: FactorioPath? = null): FactorioPath? {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        if (SystemInfo.isMac) {
            descriptor.isForcedToUseIdeaFileChooser = true
        }

        val initial = if (factorioPath != null) {
            LocalFileSystem.getInstance().findFileByPath(factorioPath.systemIndependentPath)
        } else {
            null
        }

        return FileChooser.chooseFile(descriptor, project, initial)?.let {
            FactorioPath(it.path)
        }
    }

    fun showAndGetSelected(initialSelection: FactorioPathRef?): Ref<FactorioPathRef>? {
        if (initialSelection != null && initialSelection != FactorioPathField.NO_FACTORIO_PATH_REF) {
            addIfNeededAndSelect(initialSelection)
        } else {
            list.clearSelection()
        }

        val pathManager = service<FactorioPathManager>()
        if (!showAndGet()) {
            if (prevProjectFactorioPathRef != null) {
                pathManager.setProjectFactorioPath(project, prevProjectFactorioPathRef)
            }

            return null
        } else {
            val modifiedPaths = arrayListOf<FactorioPath>()
            getFactorioPathRefs().forEach {
                val path = changesMap[it] ?: it.resolve(project)
                if (path != null) {
                    modifiedPaths.add(path)
                }
            }
            pathManager.setFactorioPaths(modifiedPaths)

            return Ref.create(list.selectedValue as FactorioPathRef)
        }
    }

    private fun addIfNeededAndSelect(pathRef: FactorioPathRef) {
        var index = listModel.indexOf(pathRef)
        if (index == -1) {
            listModel.addElement(pathRef)
            index = listModel.size() - 1
        }
        list.selectedIndex = index
        UiNotifyConnector.doWhenFirstShown(list) {
            ScrollingUtil.ensureIndexIsVisible(list, index, 1)
        }
    }

    private inner class SetAsProjectDefaultFactorioPathAction :
        DumbAwareAction(YAFCBundle.message("yafc.set.project.factorio.path.action")) {

        override fun update(e: AnActionEvent) {
            val ref = getSingleSelectedFactorioPathRef()
            e.presentation.isEnabledAndVisible = canUseAsProjectDefaultFactorioPathRef(ref)
        }

        private fun canUseAsProjectDefaultFactorioPathRef(pathRef: FactorioPathRef?): Boolean {
            return (pathRef != null && !pathRef.isProjectRef())
        }

        override fun actionPerformed(e: AnActionEvent) {
            val ref = getSingleSelectedFactorioPathRef()
            if (canUseAsProjectDefaultFactorioPathRef(ref)) {
                service<FactorioPathManager>().setProjectFactorioPath(project, ref)
                list.repaint()
            }
        }
    }

    companion object {
        private const val DIMENSION_SERVICE_KEY = "yafc.factorio-paths-dialog"
    }
}
