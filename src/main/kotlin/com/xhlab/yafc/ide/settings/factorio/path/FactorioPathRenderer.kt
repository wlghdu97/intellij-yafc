package com.xhlab.yafc.ide.settings.factorio.path

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.text.SemVer
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI.Borders
import com.intellij.util.ui.UIUtil
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.ide.util.LeftRightJustifyingLayoutManager
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.border.EmptyBorder

class FactorioPathRenderer constructor(
    private val project: Project,
    compact: Boolean
) : ListCellRenderer<FactorioPathRef> {
    private val panel = JPanel(GridBagLayout())
    private val nameComp = SimpleColoredComponent()
    private val versionComp = SimpleColoredComponent()

    init {
        with(nameComp) {
            isOpaque = false
            ipad = JBInsets(0, 0, 0, 0)
        }
        with(versionComp) {
            isOpaque = false
            ipad = JBInsets(0, 10, 0, 0)
        }

        val insets = if (compact) {
            JBInsets(0, 1, 0, 1)
        } else {
            val horizontalInset = UIUtil.getListCellHPadding()
            JBInsets(2, horizontalInset, 2, horizontalInset)
        }
        panel.border = IdeBorderFactory.createEmptyBorder(insets)

        if (compact) {
            adjustBorderHeight(nameComp)
            versionComp.myBorder = null
        }

        val nameContainer = wrapInLeftRightJustifyingContainer(nameComp)
        panel.add(
            nameContainer,
            GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 256, 2, JBInsets(0, 0, 0, 0), 0, 0)
        )
        panel.add(
            versionComp,
            GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 256, 0, JBInsets(0, 0, 0, 0), 0, 0)
        )
    }

    private fun adjustBorderHeight(component: SimpleColoredComponent) {
        component.myBorder = EmptyBorder(1, 0, 1, 0)
    }

    private fun wrapInLeftRightJustifyingContainer(component: SimpleColoredComponent): JPanel {
        val container = object : JPanel(LeftRightJustifyingLayoutManager()) {
            override fun getBaseline(width: Int, height: Int): Int {
                return component.getBaseline(width, height)
            }
        }
        with(container) {
            border = Borders.empty()
            isOpaque = false
            add(component)
        }
        return container
    }

    override fun getListCellRendererComponent(
        list: JList<out FactorioPathRef>,
        value: FactorioPathRef?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        nameComp.clear()
        versionComp.clear()

        val baseFont = list.font
        nameComp.font = baseFont
        versionComp.font = UIUtil.getFont(UIUtil.FontSize.SMALL, baseFont)

        if (panel.isEnabled != list.isEnabled) {
            UIUtil.setEnabled(panel, list.isEnabled, true)
        }

        return panel.apply {
            if (value != null) {
                panel.background = if (isSelected) list.selectionBackground else list.background
                val foreground = if (isSelected) list.selectionForeground else list.foreground
                nameComp.foreground = foreground
                versionComp.foreground = foreground
                customize(list, value, index, isSelected)
            }
        }
    }

    private fun customize(
        list: JList<out FactorioPathRef?>,
        pathRef: FactorioPathRef,
        index: Int,
        isSelected: Boolean
    ) {
        if (pathRef == FactorioPathField.NO_FACTORIO_PATH_REF) {
            nameComp.append(
                YAFCBundle.message("yafc.no.factorio.path"),
                SimpleTextAttributes.ERROR_ATTRIBUTES
            )
        } else {
            val path = pathRef.resolve(project)
            val versionRef: Ref<SemVer>? = path?.getCachedVersion()

            var errorMessage: String? = null
            if (versionRef != null && versionRef.isNull) {
                errorMessage = path.validate()
            }
            panel.toolTipText = errorMessage

            if (path != null) {
                val valid = (errorMessage == null)
                val attributes = if (valid) {
                    SimpleTextAttributes.REGULAR_ATTRIBUTES
                } else {
                    SimpleTextAttributes.ERROR_ATTRIBUTES
                }
                nameComp.append(path.presentableName, attributes)
            } else {
                nameComp.append(
                    "${pathRef.referenceName} (${YAFCBundle.message("yafc.no.factorio.path.reference.found")})",
                    SimpleTextAttributes.ERROR_ATTRIBUTES
                )
            }

            if (versionRef != null) {
                val version = versionRef.get()
                if (version != null) {
                    addVersion(version, isSelected)
                }
            } else if (path != null) {
                val modalityState = ModalityState.current()
                path.fetchVersion { version ->
                    if (version != null) {
                        ApplicationManager.getApplication().invokeLater({
                            scheduleRepaint(list, pathRef, index)
                        }, modalityState)
                    }
                }
            }
        }
    }

    private fun addVersion(version: SemVer, isSelected: Boolean) {
        val attrs = if (isSelected) SimpleTextAttributes.REGULAR_ATTRIBUTES else SimpleTextAttributes.GRAYED_ATTRIBUTES
        versionComp.append(version.rawVersion, attrs)
    }

    private fun scheduleRepaint(list: JList<out FactorioPathRef?>, pathRef: FactorioPathRef, index: Int) {
        if (list.isVisible) {
            val listModel = list.model
            if (index == -1) {
                if (listModel is FactorioPathField.FactorioPathComboBoxModel) {
                    listModel.repaintSelectedElementIfMatches(pathRef)
                }
            } else if (index in 0 until listModel.size) {
                val element = listModel.getElementAt(index)
                if (element == pathRef) {
                    list.repaint(list.getCellBounds(index, index))
                }
            }
        }
    }
}
