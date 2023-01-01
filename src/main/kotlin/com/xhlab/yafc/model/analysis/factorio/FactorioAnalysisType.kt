package com.xhlab.yafc.model.analysis.factorio

import com.xhlab.yafc.model.analysis.YAFCAnalysisType

enum class FactorioAnalysisType(override val presentedName: String) : YAFCAnalysisType {
    MILESTONES("Milestones"),
    AUTOMATION("Automation"),
    COST("Cost"),
    COST_ONLY_CURRENT_MILESTONES("Cost Only Current Milestones"),
    TECHNOLOGY_SCIENCE("Technology Science");
}
