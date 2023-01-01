package com.xhlab.yafc.model

import com.xhlab.yafc.model.analysis.YAFCDependencies
import com.xhlab.yafc.model.analysis.factorio.FactorioAnalyses
import com.xhlab.yafc.model.data.YAFCDatabase

data class YAFCStorage(
    val db: YAFCDatabase,
    val dependencies: YAFCDependencies,
    val analyses: FactorioAnalyses
)
