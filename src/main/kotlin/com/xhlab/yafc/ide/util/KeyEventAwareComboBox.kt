package com.xhlab.yafc.ide.util

import com.intellij.openapi.ui.ComboBox
import java.awt.event.KeyEvent

class KeyEventAwareComboBox<T> : ComboBox<T>() {
    private var myKeyEventProcessing = false
    private var myItemStateChangedEventsAllowed = true

    override fun processKeyEvent(e: KeyEvent) {
        myKeyEventProcessing = true
        super.processKeyEvent(e)
        myKeyEventProcessing = false
    }

    override fun selectedItemChanged() {
        if (myItemStateChangedEventsAllowed) {
            super.selectedItemChanged()
        }
    }

    fun doWithItemStateChangedEventsDisabled(block: () -> Unit) {
        myItemStateChangedEventsAllowed = false
        try {
            block.invoke()
        } finally {
            myItemStateChangedEventsAllowed = true
        }
    }
}
