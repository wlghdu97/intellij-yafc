package com.xhlab.yafc.ide.settings.yafc

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.model.ProjectPerItemFlag
import com.xhlab.yafc.model.YAFCProject
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.data.DataUtils
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.util.toSet
import javax.swing.JTable
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

data class YAFCMilestoneDescriptor(
    val target: FactorioObject,
    var isEnabled: Boolean
)

/**
 * @see org.jetbrains.kotlin.idea.script.configuration.KotlinScriptDefinitionsModel
 */
class YAFCMilestoneModel private constructor(
    definitions: MutableList<YAFCMilestoneDescriptor>
) : ListTableModel<YAFCMilestoneDescriptor>(arrayOf(MilestoneEnabled(), MilestoneName()), definitions, 0) {

    private class MilestoneEnabled : ColumnInfo<YAFCMilestoneDescriptor, Boolean>(
        YAFCBundle.message("settings.yafc.milestones.descriptor.enabled")
    ) {
        override fun getEditor(item: YAFCMilestoneDescriptor?): TableCellEditor = BooleanTableCellEditor()
        override fun getRenderer(item: YAFCMilestoneDescriptor?): TableCellRenderer = BooleanTableCellRenderer()
        override fun getWidth(table: JTable?): Int = 50

        override fun valueOf(item: YAFCMilestoneDescriptor): Boolean {
            return item.isEnabled
        }

        override fun setValue(item: YAFCMilestoneDescriptor, value: Boolean) {
            item.isEnabled = value
        }

        override fun isCellEditable(item: YAFCMilestoneDescriptor?): Boolean = true
    }

    private class MilestoneName : ColumnInfo<YAFCMilestoneDescriptor, FactorioObject>(
        YAFCBundle.message("settings.yafc.milestones.descriptor.name")
    ) {
        private val milestoneNameRenderer = object : ColoredTableCellRenderer() {
            override fun customizeCellRenderer(
                table: JTable,
                value: Any?,
                selected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ) {
                if (value is FactorioObject) {
                    append(value.locName, SimpleTextAttributes.REGULAR_ATTRIBUTES, true)
                }
            }
        }

        override fun valueOf(item: YAFCMilestoneDescriptor?): FactorioObject? {
            return item?.target
        }

        override fun getRenderer(item: YAFCMilestoneDescriptor?): TableCellRenderer {
            return milestoneNameRenderer
        }
    }

    companion object {
        fun createModel(project: Project): YAFCMilestoneModel {
            return YAFCMilestoneModel(createModelItems(project))
        }

        fun createModelItems(project: Project): MutableList<YAFCMilestoneDescriptor> {
            val db = project.service<YAFCProject>().storage?.db
                ?: return arrayListOf()

            val settings = project.service<YAFCProjectSettings>()
            return settings.milestones.asSequence()
                .mapNotNull { typeDotName -> db.objects.all.find { it.typeDotName == typeDotName } }
                .map {
                    YAFCMilestoneDescriptor(
                        it, DataUtils.hasFlags(
                            settings.flags(it.typeDotName),
                            ProjectPerItemFlag.MILESTONE_UNLOCKED.toSet()
                        )
                    )
                }.toMutableList()
        }
    }
}
