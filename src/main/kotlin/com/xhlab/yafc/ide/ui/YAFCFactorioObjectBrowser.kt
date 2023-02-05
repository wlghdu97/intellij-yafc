package com.xhlab.yafc.ide.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.speedSearch.NameFilteringListModel
import com.intellij.ui.speedSearch.SpeedSearch
import com.xhlab.yafc.ide.project.sync.YAFCSyncListener
import com.xhlab.yafc.model.YAFCProject
import com.xhlab.yafc.model.data.FactorioObject
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class YAFCFactorioObjectBrowser(
    private val project: Project
) : SimpleToolWindowPanel(true, true), YAFCSyncListener, Disposable {
    private val searchField = SearchTextField()
    private val speedSearch = SpeedSearch(true)

    private val list = JBList<FactorioObject>()
    private val listModel = DefaultListModel<FactorioObject>()
    private val filteringListModel = NameFilteringListModel(
        listModel,
        { it.locName },
        speedSearch::shouldBeShowing,
        { StringUtil.notNullize(speedSearch.filter) }
    )
    private val renderer = YAFCFactorioObjectListCellRenderer(project, FactorioObjectCellType.NORMAL)
    private val listMouseAdapter = FactorioObjectMouseAdapter(project, list, renderer, filteringListModel, false)
    private val scrollPane = JBScrollPane(list)

    private val connection = project.messageBus.connect()

    init {
        layout = BorderLayout()

        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                speedSearch.updatePattern(e.document.getText(0, e.document.length))
            }
        })

        speedSearch.addChangeListener {
            filteringListModel.refilter()
        }

        toolbar = JPanel(BorderLayout()).apply {
            add(searchField)
        }

        with(list) {
            emptyText.text = "DB Sync is not done"
            model = filteringListModel
            cellRenderer = renderer

            addMouseListener(listMouseAdapter)
            addMouseMotionListener(listMouseAdapter)
        }
        add(scrollPane.apply {
            verticalScrollBar.addAdjustmentListener {
                listMouseAdapter.hideCurrentTooltip()
            }
        })

        populateTable()

        connection.subscribe(YAFCProject.YAFC_SYNC_TOPIC, this)
    }

    override fun syncFailed(project: Project) = resetTable()

    override fun syncNeeded(project: Project) = resetTable()

    override fun syncStarted(project: Project) = resetTable()

    override fun syncSucceeded(project: Project) {
        populateTable()
    }

    private fun resetTable() {
        listModel.removeAllElements()
    }

    private fun populateTable() {
        resetTable()
        val storage = project.service<YAFCProject>().storage
        if (storage != null) {
            listModel.addAll(storage.db.objects.all)
        }
    }

    override fun dispose() {
        connection.disconnect()
    }
}
