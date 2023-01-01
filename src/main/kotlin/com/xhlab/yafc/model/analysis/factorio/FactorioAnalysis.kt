package com.xhlab.yafc.model.analysis.factorio

import com.xhlab.yafc.model.analysis.YAFCAnalysis

abstract class FactorioAnalysis constructor(override val type: FactorioAnalysisType) :
    YAFCAnalysis<FactorioAnalysisType>
