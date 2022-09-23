package com.xhlab.yafc.ide.util

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

class LeftRightJustifyingLayoutManager : LayoutManager {

    override fun addLayoutComponent(name: String, comp: Component) = Unit

    override fun removeLayoutComponent(comp: Component) = Unit

    override fun preferredLayoutSize(container: Container): Dimension {
        val child = getChildComponent(container)
        val dim = child?.preferredSize
        return calcLayoutSize(container, dim)
    }

    override fun minimumLayoutSize(container: Container): Dimension {
        val child = getChildComponent(container)
        val dim = child?.minimumSize
        return calcLayoutSize(container, dim)
    }

    override fun layoutContainer(container: Container) {
        val child = getChildComponent(container)
        if (child != null) {
            val pref = child.preferredSize
            val insets = container.insets
            val availableWidth = container.width - insets.left - insets.right
            if (pref.width <= availableWidth) {
                child.setBounds(insets.left, insets.top, pref.width, pref.height)
            } else {
                child.setBounds(availableWidth + insets.left - pref.width, insets.top, pref.width, pref.height)
            }
        }
    }

    companion object {
        private fun getChildComponent(container: Container): Component? {
            return if (container.componentCount > 0) container.getComponent(0) else null
        }

        private fun calcLayoutSize(container: Container, childComponentDimension: Dimension?): Dimension {
            val insets = container.insets
            val dim = Dimension(insets.left + insets.right, insets.top + insets.bottom)
            if (childComponentDimension != null) {
                dim.width += childComponentDimension.width
                dim.height += childComponentDimension.height
            }
            return dim
        }
    }
}
