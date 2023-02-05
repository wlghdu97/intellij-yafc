package com.xhlab.yafc.ide.ui

import com.intellij.ui.LayeredIcon
import com.intellij.ui.RowIcon
import com.intellij.util.IconUtil
import com.xhlab.yafc.model.YAFCStorage
import com.xhlab.yafc.model.analysis.factorio.FactorioAnalysisType
import com.xhlab.yafc.model.analysis.factorio.FactorioMilestones
import com.xhlab.yafc.model.data.FactorioObject
import javax.swing.Icon

fun IconCollection.getIconWithHighestMilestoneBadge(
    storage: YAFCStorage,
    element: FactorioObject,
    cellType: FactorioObjectCellType
): Icon {
    val primary = when (cellType) {
        FactorioObjectCellType.NORMAL -> {
            getIcon(storage.dataSource, element)
        }

        FactorioObjectCellType.BIG -> {
            getBigIcon(storage.dataSource, element)
        }
    } ?: IconUtil.getEmptyIcon(false)

    val milestones = storage.analyses.get<FactorioMilestones>(FactorioAnalysisType.MILESTONES)
    return if (milestones != null) {
        val secondary = milestones.highestMilestone[element]?.let {
            when (cellType) {
                FactorioObjectCellType.NORMAL -> {
                    getSmallIcon(storage.dataSource, it)
                }

                FactorioObjectCellType.BIG -> {
                    getSmallIcon(
                        storage.dataSource,
                        it,
                        IconCollection.IconSize.BIG,
                        IconCollection.IconGravity.RIGHT_BOTTOM
                    )
                }
            }
        } ?: IconUtil.getEmptyIcon(false)

        when (cellType) {
            FactorioObjectCellType.NORMAL -> {
                RowIcon(primary, secondary)
            }

            FactorioObjectCellType.BIG -> {
                LayeredIcon(primary, secondary)
            }
        }
    } else {
        primary
    }
}
