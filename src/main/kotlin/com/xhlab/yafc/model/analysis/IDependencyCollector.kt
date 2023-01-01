package com.xhlab.yafc.model.analysis

import com.xhlab.yafc.model.data.FactorioId
import com.xhlab.yafc.model.data.FactorioObject

interface IDependencyCollector {
    fun addId(raw: List<FactorioId>, flags: DependencyListFlags)
    fun addObject(raw: List<FactorioObject>, flags: DependencyListFlags)
}
