package com.xhlab.yafc.ide.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.speedSearch.NameFilteringListModel
import com.intellij.ui.speedSearch.SpeedSearch
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.model.data.FactorioObject
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent

class YAFCObjectChooserDialog constructor(
    private val project: Project,
    targetObject: String,
    private val objectList: List<FactorioObject>,
    private val excluded: Set<FactorioObject>,
    canSelectMultiple: Boolean = false
) : DialogWrapper(project), YAFCObjectChooser {
    private val mainPanel = JPanel(BorderLayout())
    private val list = JBList<FactorioObject>().apply {
        selectionMode = if (canSelectMultiple) {
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        } else {
            ListSelectionModel.SINGLE_SELECTION
        }
    }
    private val listModel = DefaultListModel<FactorioObject>()

    private val searchField = SearchTextField()
    private val speedSearch = SpeedSearch(true)

    init {
        title = YAFCBundle.message("yafc.object.chooser.title", targetObject)
        init()
    }

    override val selectedObjects: List<FactorioObject>
        get() = list.selectedValuesList.toList()

    override fun doValidate(): ValidationInfo? {
        return if (list.isSelectionEmpty) {
            ValidationInfo(YAFCBundle.message("yafc.object.chooser.object.not.selected.dialog.message"), null)
        } else {
            super.doValidate()
        }
    }

    override fun createCenterPanel(): JComponent {
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                speedSearch.updatePattern(e.document.getText(0, e.document.length))
            }
        })
        val filteringListModel = NameFilteringListModel(
            listModel,
            { it.locName },
            speedSearch::shouldBeShowing,
            { StringUtil.notNullize(speedSearch.filter) }
        )
        speedSearch.addChangeListener {
            filteringListModel.refilter()
        }
        mainPanel.add(searchField, BorderLayout.NORTH)

        with(list) {
            emptyText.text = "DB Sync is not done"
            model = filteringListModel
            cellRenderer = YAFCFactorioObjectListCellRenderer(project)
        }
        mainPanel.add(JBScrollPane(list))
        populateTable()

        return mainPanel
    }

    private fun populateTable() {
        listModel.addAll(objectList.minus(excluded))
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return searchField
    }
}
