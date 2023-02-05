package com.xhlab.yafc.ide.ui

import com.xhlab.yafc.model.data.FactorioObject

interface FactorioObjectHint {
    val element: FactorioObject
    fun show()
    fun hideCurrent()
}
