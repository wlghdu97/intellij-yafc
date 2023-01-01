package com.xhlab.yafc.model.analysis.factorio

import com.xhlab.yafc.model.ErrorCollector
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.analysis.YAFCDependencies
import com.xhlab.yafc.model.data.*
import java.util.*

class FactorioTechnologyScienceAnalysis constructor(
    private val db: YAFCDatabase,
    private val dependencies: YAFCDependencies,
    private val milestones: FactorioMilestones
) : FactorioAnalysis(FactorioAnalysisType.TECHNOLOGY_SCIENCE) {
    val allSciencePacks = db.technologies.createMapping<List<Ingredient>>()

    fun getMaxTechnologyIngredient(tech: Technology): Ingredient? {
        val list = allSciencePacks[tech]
        var ingredient: Ingredient? = null
        var order = 0uL
        for (elem in list ?: emptyList()) {
            val elemOrder = (milestones.milestoneResult[elem.goods.id] ?: 0uL) - 1uL
            if (ingredient == null || elemOrder > order) {
                order = elemOrder
                ingredient = elem
            }
        }
        return ingredient
    }

    override fun compute(settings: YAFCProjectSettings, errorCollector: ErrorCollector) {
        allSciencePacks.clear()

        val sciencePacks = db.allSciencePacks
        val sciencePackIndex = db.goods.createMapping { 0 }
        for (i in sciencePacks.indices) {
            sciencePackIndex[sciencePacks[i]] = i
        }
        val sciencePackCount = Array<Mapping<Technology, Float>>(sciencePacks.size) {
            db.technologies.createMapping()
        }

        val processing = db.technologies.createMapping { false }
        val requirementMap = db.technologies.createDoubleMapping { _, _ -> false }

        val queue: Queue<Technology> = LinkedList()
        for (tech in db.technologies.all) {
            if (tech.prerequisites.isEmpty()) {
                processing[tech] = true
                queue.add(tech)
            }
        }
        val prerequisiteQueue: Queue<Technology> = LinkedList()

        while (queue.size > 0) {
            val current = queue.poll()

            // Fast processing for the first prerequisite (just copy everything)
            if (current.prerequisites.isNotEmpty()) {
                val firstRequirement = current.prerequisites[0]
                for (pack in sciencePackCount) {
                    pack[current] = (pack[current] ?: 0f) + (pack[firstRequirement] ?: 0f)
                }
                requirementMap.copyRow(firstRequirement, current)
            }

            requirementMap[current to current] = true
            prerequisiteQueue.add(current)

            while (prerequisiteQueue.size > 0) {
                val prerequisite = prerequisiteQueue.poll()
                for (ingredient in prerequisite.ingredients) {
                    val science = sciencePackIndex[ingredient.goods]
                    if (science != null) {
                        val sciencePack = sciencePackCount[science]
                        sciencePack[current] = (sciencePack[current] ?: 0f) + (ingredient.amount * prerequisite.count)
                    }
                }

                for (prerequisitePrerequisite in prerequisite.prerequisites) {
                    if (requirementMap[current to prerequisitePrerequisite] == false) {
                        prerequisiteQueue.add(prerequisitePrerequisite)
                        requirementMap[current to prerequisitePrerequisite] = true
                    }
                }
            }

            // queue next unlocking technologies
            for (unlocks in dependencies.reverseDependencies[current] ?: emptyList()) {
                val tech = db.objects[unlocks]
                if (tech is Technology && processing[tech] == false) {
                    run {
                        for (techPreq in tech.prerequisites) {
                            if (processing[techPreq] == false) {
                                return@run
                            }
                        }

                        processing[tech] = true
                        queue.add(tech)
                    }
                }
            }
        }

        allSciencePacks.keys.forEach { tech ->
            allSciencePacks[tech] = sciencePackCount.mapIndexed { idx, x ->
                val count = x[tech] ?: 0f
                if (count == 0f) {
                    null
                } else {
                    MutableIngredient(sciencePacks[idx] as MutableGoods, count)
                }
            }.mapNotNull { it }
        }
    }

    override val description =
        "Technology analysis calculates the total amount of science packs required for each technology"
}
