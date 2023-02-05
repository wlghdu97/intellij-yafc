package com.xhlab.yafc.ide.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.speedSearch.FilteringListModel
import com.intellij.util.ui.UIUtil
import com.xhlab.yafc.model.YAFCProject
import com.xhlab.yafc.model.YAFCStorage
import com.xhlab.yafc.model.data.FactorioObject
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JList

class FactorioObjectMouseAdapter(
    private val project: Project,
    private val list: JList<FactorioObject>,
    private val renderer: YAFCFactorioObjectListCellRenderer,
    private val filteringListModel: FilteringListModel<FactorioObject>,
    private val showAsPopup: Boolean
) : MouseAdapter(), Disposable {
    private var currentHint: FactorioObjectHint? = null

    override fun mouseMoved(e: MouseEvent) {
        if (filteringListModel.size == 0) {
            return
        }
        val index = list.locationToIndex(e.point)
        if (index == -1) {
            return
        }
        val element = filteringListModel.getElementAt(index) ?: return
        if (currentHint?.element == element) {
            return
        }
        currentHint?.hideCurrent()

        val tipComponent = createHint(project, element)
        val height = renderer.preferredSize.height
        val point = Point(0, index * height)

        val hint = if (showAsPopup) {
            FactorioObjectPopup(element, list, tipComponent, point)
        } else {
            FactorioObjectTooltip(element, list, tipComponent, point.apply { y += (height / 2) })
        }.apply {
            show()
        }

        currentHint = hint
    }

    override fun mouseExited(e: MouseEvent?) {
        currentHint?.hideCurrent()
    }

    override fun dispose() = Unit

    fun hideCurrentTooltip() {
        currentHint?.hideCurrent()
    }

    private fun createHint(project: Project, element: FactorioObject): JComponent {
        val storage = project.service<YAFCProject>().storage
        return if (storage != null) {
            createDetailedHint(storage, element)
        } else {
            createSimpleHint(element)
        }
    }

    private fun createDetailedHint(storage: YAFCStorage, element: FactorioObject): JComponent {
        return FactorioObjectDetailedHint.create(project, storage, element)
    }

    private fun createSimpleHint(element: FactorioObject): JComponent {
        return panel {
            row(element.locName) {
                if (element.locDescr.isNotEmpty()) {
                    rowComment(element.locDescr)
                }
            }
        }.withPreferredWidth(JBUIScale.scale(320)).apply {
            background = UIUtil.getToolTipBackground()
        }
    }
}
