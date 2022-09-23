package com.xhlab.yafc.ide.settings.factorio.modpath

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

open class FactorioModPathField constructor(
    private val project: Project
) : ComponentWithBrowseButton<ComboBox<FactorioModPathRef>>(KeyEventAwareComboBox(), null) {
    private val comboBox = this.childComponent as KeyEventAwareComboBox<FactorioModPathRef>
    private val model = FactorioModPathComboBoxModel()
    private val changeListeners: MutableList<FactorioModPathChangeListener> =
        ContainerUtil.createLockFreeCopyOnWriteList()
    private var lastSelectedItem = NO_FACTORIO_MOD_PATH_REF
    private var dropDownListUpdateRequested = false

    init {
        addActionListener {
            showFactorioModPathDialog()
        }
        with(childComponent) {
            model = this@FactorioModPathField.model
            renderer = FactorioModPathRenderer(project)
            setMinimumAndPreferredWidth(0)
        }
        with(model) {
            addElement(NO_FACTORIO_MOD_PATH_REF)
            selectedItem = NO_FACTORIO_MOD_PATH_REF
            requestDropDownListUpdate()
        }
        comboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                handleSelectedItemChange()
            }
        }
    }

    private fun handleSelectedItemChange() {
        val selectedItem = getFactorioModPathRef()
        if (lastSelectedItem != selectedItem) {
            lastSelectedItem = selectedItem
            val path = selectedItem.resolve(project)
            changeListeners.forEach {
                it.factorioModPathChanged(path)
            }
        }
    }

    private fun showFactorioModPathDialog() {
        val dialog = FactorioModPathDialog(project, isDefaultProjectFactorioModPathField())
        val value = dialog.showAndGetSelected(model.selectedItem as? FactorioModPathRef)
        if (value != null) {
            if (value.get() != null) {
                setFactorioModPathRef(value.get())
            }

            model.repaintSelectedElementIfMatches(getFactorioModPathRef())
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
        val refList = ArrayList(service<FactorioModPathManager>().getFactorioModPaths().map { it.toRef() })
        val selectedRef = getFactorioModPathRef()
        if (!refList.contains(selectedRef)) {
            refList.add(selectedRef)
        }
        if (!refList.contains(NO_FACTORIO_MOD_PATH_REF)) {
            refList.add(NO_FACTORIO_MOD_PATH_REF)
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

    open fun isDefaultProjectFactorioModPathField(): Boolean {
        return false
    }

    fun setFactorioModPathRef(modPathRef: FactorioModPathRef) {
        if (isDefaultProjectFactorioModPathField() && modPathRef.isProjectRef()) {
            throw IllegalArgumentException("Project default factorio mod path field cannot be set to : ${modPathRef.referenceName}")
        } else {
            model.selectedItem = modPathRef
        }
    }

    fun getFactorioModPathRef(): FactorioModPathRef {
        val modPathRef = comboBox.selectedItem as? FactorioModPathRef
        return if (modPathRef == null) {
            logger.warn("No factorio mod path ref")
            NO_FACTORIO_MOD_PATH_REF
        } else {
            modPathRef
        }
    }

    fun addChangeListener(listener: FactorioModPathChangeListener) {
        changeListeners.add(listener)
    }

    class FactorioModPathComboBoxModel : DefaultComboBoxModel<FactorioModPathRef>() {

        fun repaintSelectedElementIfMatches(pathRef: FactorioModPathRef) {
            if (selectedItem == pathRef) {
                fireContentsChanged(this, -1, -1)
            }
        }
    }

    companion object {
        private val logger = Logger.getInstance(FactorioModPathField::class.java)
        val NO_FACTORIO_MOD_PATH_REF = FactorioModPathRef.create("")

        val labelTextForComponent: String
            get() = YAFCBundle.message("yafc.factorio.mod.path.label")
    }
}
