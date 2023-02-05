package com.xhlab.yafc.ide.ui

import com.intellij.ide.IdeTooltip
import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.components.panels.Wrapper
import com.xhlab.yafc.model.data.FactorioObject
import java.awt.Point
import javax.swing.JComponent

class FactorioObjectTooltip(
    override val element: FactorioObject,
    private val component: JComponent,
    private val content: JComponent,
    private val point: Point
) : FactorioObjectHint {
    private var tooltip: IdeTooltip? = null

    override fun show() {
        val balloonPosition = calculateBalloonPosition()
        val actualPoint = point.apply {
            if (balloonPosition == Balloon.Position.atRight) {
                x += component.parent.width
            }
        }
        val tooltip = object : IdeTooltip(component, actualPoint, Wrapper(content)) {
            override fun canBeDismissedOnTimeout() = false
        }.setPreferredPosition(balloonPosition)

        IdeTooltipManager.getInstance().show(tooltip, true, false)
    }

    private fun calculateBalloonPosition(): Balloon.Position {
        val componentCenterX = component.locationOnScreen.x + (component.width / 2)
        val rootCenterX = component.rootPane.locationOnScreen.x + (component.rootPane.width / 2)
        return if (componentCenterX > rootCenterX) {
            Balloon.Position.atLeft
        } else {
            Balloon.Position.atRight
        }
    }

    override fun hideCurrent() {
        tooltip?.let {
            IdeTooltipManager.getInstance().hide(it)
        }
    }
}
