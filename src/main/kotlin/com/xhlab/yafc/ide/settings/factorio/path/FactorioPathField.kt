package com.xhlab.yafc.ide.settings.factorio.path

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.SwingHelper
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.ide.util.KeyEventAwareComboBox
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.event.PopupMenuEvent

open class FactorioPathField constructor(
    private val project: Project
) : ComponentWithBrowseButton<ComboBox<FactorioPathRef>>(KeyEventAwareComboBox(), null) {
    private val comboBox = this.childComponent as KeyEventAwareComboBox<FactorioPathRef>
    private val model = FactorioPathComboBoxModel()
    private val changeListeners: MutableList<FactorioPathChangeListener> = ContainerUtil.createLockFreeCopyOnWriteList()
    private var lastSelectedItem = NO_FACTORIO_PATH_REF
    private var dropDownListUpdateRequested = false

    init {
        addActionListener {
            showFactorioPathDialog()
        }
        with(comboBox) {
            model = this@FactorioPathField.model
            renderer = FactorioPathRenderer(project, true)
            setMinimumAndPreferredWidth(0)
        }
        with(model) {
            addElement(NO_FACTORIO_PATH_REF)
            selectedItem = NO_FACTORIO_PATH_REF
            requestDropDownListUpdate()
        }
        comboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                handleSelectedItemChange()
            }
        }
    }

    private fun handleSelectedItemChange() {
        val selectedItem = getFactorioPathRef()
        if (lastSelectedItem != selectedItem) {
            lastSelectedItem = selectedItem
            val path = selectedItem.resolve(project)
            changeListeners.forEach {
                it.factorioPathChanged(path)
            }
        }
    }

    private fun showFactorioPathDialog() {
        val dialog = FactorioPathDialog(project, isDefaultProjectFactorioPathField())
        val value = dialog.showAndGetSelected(model.selectedItem as? FactorioPathRef)
        if (value != null) {
            if (value.get() != null) {
                setFactorioPathRef(value.get())
            }

            model.repaintSelectedElementIfMatches(getFactorioPathRef())
            requestDropDownListUpdate()
        }
    }

    private fun requestDropDownListUpdate() {
        if (!dropDownListUpdateRequested) {
            dropDownListUpdateRequested = true
            comboBox.addPopupMenuListener(object : PopupMenuListenerAdapter() {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                    comboBox.removePopupMenuListener(this)
                    updateDropDownList()
                    dropDownListUpdateRequested = false
                }
            })
        }
    }

    private fun updateDropDownList() {
        val refList = ArrayList(service<FactorioPathManager>().getFactorioPaths().map { it.toRef() })
        val selectedRef = getFactorioPathRef()
        if (!refList.contains(selectedRef)) {
            refList.add(selectedRef)
        }
        comboBox.doWithItemStateChangedEventsDisabled {
            SwingHelper.updateItems(comboBox, refList, null)
        }
        if (comboBox.selectedItem != selectedRef) {
            comboBox.selectedItem = selectedRef
            handleSelectedItemChange()
        }
    }

    final override fun addActionListener(listener: ActionListener?) {
        super.addActionListener(listener)
    }

    open fun isDefaultProjectFactorioPathField(): Boolean {
        return false
    }

    fun setFactorioPathRef(pathRef: FactorioPathRef) {
        if (isDefaultProjectFactorioPathField() && pathRef.isProjectRef()) {
            throw IllegalArgumentException("Project default factorio path field cannot be set to '${pathRef.referenceName}' value")
        } else {
            model.selectedItem = pathRef
        }
    }

    fun getFactorioPathRef(): FactorioPathRef {
        val pathRef = comboBox.selectedItem as? FactorioPathRef
        return if (pathRef == null) {
            logger.warn("No factorio path ref")
            NO_FACTORIO_PATH_REF
        } else {
            pathRef
        }
    }

    fun addChangeListener(listener: FactorioPathChangeListener) {
        changeListeners.add(listener)
    }

    class FactorioPathComboBoxModel : DefaultComboBoxModel<FactorioPathRef>() {

        fun repaintSelectedElementIfMatches(pathRef: FactorioPathRef) {
            if (selectedItem == pathRef) {
                fireContentsChanged(this, -1, -1)
            }
        }
    }

    companion object {
        private val logger = Logger.getInstance(FactorioPathField::class.java)
        val NO_FACTORIO_PATH_REF = FactorioPathRef.create("")

        val labelTextForComponent: String
            get() = YAFCBundle.message("yafc.factorio.path.label")
    }
}
