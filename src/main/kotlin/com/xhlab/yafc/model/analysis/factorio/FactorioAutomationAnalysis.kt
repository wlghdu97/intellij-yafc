package com.xhlab.yafc.model.analysis.factorio

import com.intellij.openapi.diagnostic.Logger
import com.xhlab.yafc.model.ErrorCollector
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.YAFCDependencies
import com.xhlab.yafc.model.data.DataUtils
import com.xhlab.yafc.model.data.FactorioId
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.YAFCDatabase
import com.xhlab.yafc.model.util.toSet
import java.util.*

class FactorioAutomationAnalysis constructor(
    private val db: YAFCDatabase,
    private val dependencies: YAFCDependencies,
    private val milestones: FactorioMilestones
) : FactorioAnalysis(FactorioAnalysisType.AUTOMATION) {
    val automatable = db.objects.createMapping<AutomationStatus>()

    // was part of AnalysisExtensions
    fun isAutomatable(obj: FactorioObject): Boolean =
        automatable[obj] != AutomationStatus.Automatable.NOT_AUTOMATABLE

    // was part of AnalysisExtensions
    fun isAutomatableWithCurrentMilestones(obj: FactorioObject) =
        automatable[obj] == AutomationStatus.Automatable.AUTOMATABLE_NOW

    override fun compute(settings: YAFCProjectSettings, errorCollector: ErrorCollector) {
        // reset result
        automatable.fill(AutomationStatus.Unknown)
        automatable[db.voidEnergy] = AutomationStatus.Automatable.AUTOMATABLE_NOW

        val time = System.currentTimeMillis()
        val processingQueue: Queue<FactorioId> = LinkedList()
        var unknowns = 0

        // filter non-automatable recipes (such as hand-only recipes)
        for (recipe in db.recipes.all) {
            var hasAutomatableCrafter = false
            for (crafter in recipe.crafters) {
                if (crafter != db.character && milestones.isAccessible(crafter)) {
                    hasAutomatableCrafter = true
                }
            }
            if (!hasAutomatableCrafter) {
                automatable[recipe] = AutomationStatus.Automatable.NOT_AUTOMATABLE
            }
        }

        for (obj in db.objects.all) {
            if (!milestones.isAccessible(obj)) {
                automatable[obj] = AutomationStatus.Automatable.NOT_AUTOMATABLE
            } else if (automatable[obj] == AutomationStatus.Unknown) {
                unknowns += 1
                automatable[obj] = AutomationStatus.UnknownInQueue
                processingQueue.add(obj.id)
            }
        }

        while (processingQueue.size > 0) {
            val index = processingQueue.poll()
            val dependencyList = dependencies.dependencyList[index] ?: emptyArray()
            var automationState: AutomationStatus = if (milestones.isAccessibleWithCurrentMilestones(index)) {
                AutomationStatus.Automatable.AUTOMATABLE_NOW
            } else {
                AutomationStatus.Automatable.AUTOMATABLE_LATER
            }

            for (depGroup in dependencyList) {
                if (!DataUtils.hasFlags(depGroup.flags, DependencyList.Flag.ONE_TIME_INVESTMENT.toSet())) {
                    if (DataUtils.hasFlags(depGroup.flags, DependencyList.Flag.REQUIRE_EVERYTHING.toSet())) {
                        for (element in depGroup.elements) {
                            val state = automatable[element] ?: AutomationStatus.Unknown
                            if (state.value < automationState.value) {
                                automationState = state
                            }
                        }
                    } else {
                        var localHighest: AutomationStatus = AutomationStatus.Automatable.NOT_AUTOMATABLE
                        for (element in depGroup.elements) {
                            val state = automatable[element] ?: AutomationStatus.Unknown
                            if (state.value > localHighest.value) {
                                localHighest = state
                            }
                        }

                        if (localHighest.value < automationState.value) {
                            automationState = localHighest
                        }
                    }
                } else if (automationState == AutomationStatus.Automatable.AUTOMATABLE_NOW && depGroup.flags == DependencyList.Flag.CRAFTING_ENTITY.toSet()) {
                    // If only character is accessible at current milestones as a crafting entity, don't count the object as currently automatable
                    var hasMachine = false
                    for (element in depGroup.elements) {
                        if (element != db.character.id && milestones.isAccessibleWithCurrentMilestones(element)) {
                            hasMachine = true
                            break
                        }
                    }

                    if (!hasMachine) {
                        automationState = AutomationStatus.Automatable.AUTOMATABLE_LATER
                    }
                }
            }

            if (automationState == AutomationStatus.UnknownInQueue) {
                automationState = AutomationStatus.Unknown
            }

            automatable[index] = automationState
            if (automationState != AutomationStatus.Unknown) {
                unknowns -= 1
                for (revDep in dependencies.reverseDependencies[index] ?: emptySet()) {
                    val oldState = automatable[revDep]
                    if (oldState == AutomationStatus.Unknown ||
                        oldState == AutomationStatus.Automatable.AUTOMATABLE_LATER &&
                        automationState == AutomationStatus.Automatable.AUTOMATABLE_NOW
                    ) {
                        if (oldState == AutomationStatus.Automatable.AUTOMATABLE_LATER) {
                            unknowns += 1
                        }
                        processingQueue.add(revDep)
                        automatable[revDep] = AutomationStatus.UnknownInQueue
                    }
                }
            }
        }

        // finalize
        automatable[db.voidEnergy] = AutomationStatus.Automatable.NOT_AUTOMATABLE

        val elapsedTime = System.currentTimeMillis() - time
        logger.info("Automation analysis (first pass) finished in $elapsedTime ms. Unknowns left: $unknowns")

        if (unknowns > 0) {
            // TODO run graph analysis if there are any unknowns left... Right now assume they are not automatable
            for ((k, v) in automatable) {
                if (v == AutomationStatus.Unknown) {
                    automatable[k] = AutomationStatus.Automatable.NOT_AUTOMATABLE
                }
            }
        }
    }

    override val description =
        "Automation analysis tries to find what objects can be automated. Object cannot be automated if it requires looting an entity or manual crafting."

    sealed interface AutomationStatus {
        val value: Byte

        enum class Automatable constructor(override val value: Byte) : AutomationStatus {
            NOT_AUTOMATABLE(-1),
            AUTOMATABLE_LATER(2),
            AUTOMATABLE_NOW(3);
        }

        object Unknown : AutomationStatus {
            override val value: Byte = 0
        }

        object UnknownInQueue : AutomationStatus {
            override val value: Byte = 1
        }
    }

    companion object {
        private val logger = Logger.getInstance(FactorioAutomationAnalysis::class.java)
    }
}
