package com.xhlab.yafc.model.analysis

import com.xhlab.yafc.model.ErrorCollector
import com.xhlab.yafc.model.YAFCProjectSettings

interface YAFCAnalysis<TType : YAFCAnalysisType> {
    val type: TType
    val description: String
    fun compute(settings: YAFCProjectSettings, errorCollector: ErrorCollector)
}
