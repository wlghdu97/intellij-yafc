package com.xhlab.yafc.model.analysis

import com.xhlab.yafc.model.ErrorCollector
import com.xhlab.yafc.model.ErrorSeverity
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.math.Graph
import com.xhlab.yafc.model.math.topologicalSort
import com.xhlab.yafc.parser.ProgressTextIndicator
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class YAFCAnalyses<TType : YAFCAnalysisType> {
    private val typeGraph = Graph<TType>()
    private val internalAnalyses = mutableMapOf<TType, YAFCAnalysis<TType>>()
    val analyses: Map<TType, YAFCAnalysis<TType>>
        get() = internalAnalyses

    private val processingLock = ReentrantLock()

    inline operator fun <reified Analysis : YAFCAnalysis<TType>> get(type: TType): Analysis? {
        return analyses[type] as? Analysis
    }

    fun registerAnalysis(analysis: YAFCAnalysis<TType>, dependencies: List<YAFCAnalysis<TType>>) {
        internalAnalyses[analysis.type] = analysis
        dependencies.forEach {
            typeGraph.connect(it.type, analysis.type)
        }
    }

    fun processAnalyses(
        settings: YAFCProjectSettings,
        progress: ProgressTextIndicator,
        errorCollector: ErrorCollector
    ) = runAnalyses(null, settings, progress, errorCollector)

    fun recompute(
        targetType: TType,
        settings: YAFCProjectSettings,
        progress: ProgressTextIndicator,
        errorCollector: ErrorCollector
    ) = runAnalyses(targetType, settings, progress, errorCollector)

    private fun runAnalyses(
        targetType: TType?,
        settings: YAFCProjectSettings,
        progress: ProgressTextIndicator,
        errorCollector: ErrorCollector
    ) {
        processingLock.withLock {
            val processingOrder = typeGraph.topologicalSort()
            if (processingOrder == null) {
                errorCollector.appendError(
                    "Analyses has a cycle. Analysis processing is canceled.",
                    ErrorSeverity.ANALYSIS_WARNING
                )
                return@withLock
            }

            val targetedProcessingOrder = if (targetType == null) {
                processingOrder
            } else {
                val targetIdx = processingOrder.indexOf(targetType)
                if (targetIdx == -1) {
                    errorCollector.appendError(
                        "",
                        ErrorSeverity.ANALYSIS_WARNING
                    )
                    return@withLock
                }
                processingOrder.subList(targetIdx, processingOrder.size)
            }

            targetedProcessingOrder.forEach {
                val analysis = analyses[it]
                if (analysis != null) {
                    progress.setText("Running analysis algorithms", analysis.type.presentedName)
                    analysis.compute(settings, errorCollector)
                } else {
                    errorCollector.appendError(
                        "Analysis not found for : ${it.presentedName}",
                        ErrorSeverity.ANALYSIS_WARNING
                    )
                }
            }
        }
    }
}
