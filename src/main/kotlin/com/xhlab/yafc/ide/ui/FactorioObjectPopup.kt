package com.xhlab.yafc.ide.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import com.xhlab.yafc.model.data.FactorioObject
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.math.ceil

/**
 * @see com.android.tools.adtui.InformationPopup
 */
class FactorioObjectPopup(
    override val element: FactorioObject,
    private val component: JComponent,
    private val content: JComponent,
    private val point: Point
) : FactorioObjectHint, Disposable {
    private var popup: JBPopup? = null
    private var hasMouseHoveredOverPopup = false

    private val popupListener = object : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
            hasMouseHoveredOverPopup = false
        }
    }

    override fun show() {
        val tipComponent = content.apply {
            border = JBUI.Borders.empty(8)
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) {
                    hasMouseHoveredOverPopup = true
                }
            })
        }

        val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(tipComponent, null)
            .setCancelOnClickOutside(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelOnOtherWindowOpen(true)
            .setCancelOnMouseOutCallback { mouseCancelEvent ->
                hasMouseHoveredOverPopup && popup?.let { openPopup ->
                    val popupWindow = SwingUtilities.getWindowAncestor(openPopup.content)
                        ?: return@setCancelOnMouseOutCallback true
                    val currentWindow = SwingUtilities.getWindowAncestor(mouseCancelEvent.component)
                        ?: return@setCancelOnMouseOutCallback true

                    (currentWindow != popupWindow && !popupWindow.ownedWindows.contains(currentWindow))
                } ?: true
            }
            .addListener(popupListener)
            .createPopup()

        popup.show(calculatePopupPoint())
        Disposer.register(this, popup)

        this.popup = popup
    }

    private fun calculatePopupPoint(): RelativePoint {
        val componentCenterX = component.locationOnScreen.x + ceil(component.width / 2f)
        val rootCenterX = GraphicsEnvironment.getLocalGraphicsEnvironment().centerPoint.x
        return RelativePoint(component, point.apply {
            if (componentCenterX >= rootCenterX) {
                x -= content.preferredSize.width
            } else {
                x += component.width
            }
        })
    }

    override fun hideCurrent() {
        popup?.let {
            it.cancel()
            Disposer.dispose(it)
            popup = null
        }
    }

    override fun dispose() = Unit
}
