package com.xhlab.yafc.model.analysis.factorio

import com.google.ortools.linearsolver.MPConstraint
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import com.intellij.openapi.diagnostic.Logger
import com.xhlab.yafc.model.ErrorCollector
import com.xhlab.yafc.model.ErrorSeverity
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.data.*
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class FactorioCostAnalysis constructor(
    private val db: YAFCDatabase,
    private val milestones: FactorioMilestones,
    private val automation: FactorioAutomationAnalysis,
    private val onlyCurrentMilestones: Boolean
) : FactorioAnalysis(
    if (onlyCurrentMilestones) {
        FactorioAnalysisType.COST_ONLY_CURRENT_MILESTONES
    } else {
        FactorioAnalysisType.COST
    }
) {
    val cost = db.objects.createMapping<Float>()
    val recipeCost = db.recipes.createMapping<Float>()
    val recipeProductionCost = db.recipesAndTechnologies.createMapping<Float>()
    val flow = db.objects.createMapping<Float>()
    val recipeWastePercentage = db.recipes.createMapping<Float>()
    val importantItems = arrayListOf<Goods>()

    private fun shouldInclude(obj: FactorioObject): Boolean {
        return if (onlyCurrentMilestones) {
            automation.isAutomatableWithCurrentMilestones(obj)
        } else {
            automation.isAutomatable(obj)
        }
    }

    override fun compute(settings: YAFCProjectSettings, errorCollector: ErrorCollector) {
        // reset
        cost.fill(0f)
        recipeProductionCost.fill(0f)
        recipeCost.fill(0f)
        flow.fill(0f)
        recipeWastePercentage.fill(0f)

        val solver = DataUtils.createSolver("WorkspaceSolver")
        val objective = solver.objective()
        objective.setMaximization()

        val time = System.currentTimeMillis()

        val variables = db.goods.createMapping<MPVariable>()
        val constraints = db.recipes.createMapping<MPConstraint>()

        val sciencePackUsage = hashMapOf<Goods, Float>()
        for (technology in db.technologies.all) {
            if (milestones.isAccessible(technology)) {
                for (ingredient in technology.ingredients) {
                    if (automation.isAutomatable(ingredient.goods)) {
                        if (onlyCurrentMilestones && !milestones.isAccessibleAtNextMilestone(ingredient.goods)) {
                            continue
                        }
                        val prev = sciencePackUsage[ingredient.goods] ?: 0f
                        sciencePackUsage[ingredient.goods] = prev + ingredient.amount * technology.count
                    }
                }
            }
        }

        for (goods in db.goods.all) {
            if (!shouldInclude(goods)) {
                continue
            }
            var mapGeneratedAmount = 0f
            for (src in goods.miscSources) {
                if (src is Entity && src.mapGenerated) {
                    for (product in src.loot) {
                        if (product.goods == goods) {
                            mapGeneratedAmount += product.amount
                        }
                    }
                }
            }
            val variable = solver.makeVar(
                CostLowerLimit.toDouble(),
                CostLimitWhenGeneratesOnMap / mapGeneratedAmount,
                false,
                goods.name
            )
            // adding small amount to each object cost, so even objects that aren't required for science will get cost calculated
            objective.setCoefficient(variable, 1e-3)
            variables[goods] = variable
        }

        for ((item, count) in sciencePackUsage.entries) {
            objective.setCoefficient(variables[item], count / 1000.0)
        }

        val lastVariable = db.goods.createMapping<MPVariable>()
        for (recipe in db.recipes.all) {
            if (!shouldInclude(recipe)) {
                continue
            }
            if (onlyCurrentMilestones && !milestones.isAccessibleWithCurrentMilestones(recipe)) {
                continue
            }

            // TODO incorporate fuel selection. Now just select fuel if it only uses 1 fuel
            // TODO crafter and fuel list ordering impacts cost analysis result
            var singleUsedFuel: Goods? = null
            var singleUsedFuelAmount = 0f
            var minEmissions = 100f
            var minSize = 15
            var minPower = 1000f
            for (crafter in recipe.crafters) {
                minEmissions = min(crafter.energy?.emissions ?: 0f, minEmissions)
                if (crafter.energy?.type == EntityEnergyType.HEAT) {
                    break
                }
                if (crafter.size < minSize) {
                    minSize = crafter.size
                }
                val power = if (crafter.energy?.type == EntityEnergyType.VOID) {
                    0f
                } else {
                    recipe.time * crafter.power / (crafter.craftingSpeed * (crafter.energy?.effectivity ?: 0f))
                }
                if (power < minPower) {
                    minPower = power
                }
                for (fuel in crafter.energy?.fuels ?: emptyList()) {
                    if (!shouldInclude(fuel)) {
                        continue
                    }
                    if (fuel.fuelValue <= 0f) {
                        singleUsedFuel = null
                        break
                    }
                    val amount = power / fuel.fuelValue
                    if (singleUsedFuel == null) {
                        singleUsedFuel = fuel
                        singleUsedFuelAmount = amount
                    } else if (singleUsedFuel == fuel) {
                        singleUsedFuelAmount = min(singleUsedFuelAmount, amount)
                    } else {
                        singleUsedFuel = null
                        break
                    }
                }
                if (singleUsedFuel == null) {
                    break
                }
            }

            if (minPower < 0f) {
                minPower = 0f
            }
            val size = max(minSize, (recipe.ingredients.size + recipe.products.size) / 2)
            val sizeUsage = CostPerSecond * recipe.time * size
            var logisticsCost =
                (sizeUsage * (1f + CostPerIngredientPerSize * recipe.ingredients.size + CostPerProductPerSize * recipe.products.size) + CostPerMj * minPower)

            if (singleUsedFuel == db.electricity || singleUsedFuel == db.voidEnergy || singleUsedFuel == db.heat) {
                singleUsedFuel = null
            }

            val constraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0.0, recipe.name)
            constraints[recipe] = constraint

            for (product in recipe.products) {
                val variable = variables[product.goods]
                var amount = product.amount
                // was setCoefficientCheck()
                if (lastVariable[product.goods] == variable) {
                    amount += constraint.getCoefficient(variable).toFloat()
                } else {
                    lastVariable[product.goods] = variable
                }
                constraint.setCoefficient(variable, amount.toDouble())

                if (product.goods is Item) {
                    logisticsCost += product.amount * CostPerItem
                } else if (product.goods is Fluid) {
                    logisticsCost += product.amount * CostPerFluid
                }
            }

            if (singleUsedFuel != null) {
                val variable = variables[singleUsedFuel]
                // was setCoefficientCheck()
                var amount = -singleUsedFuelAmount
                if (lastVariable[singleUsedFuel] == variable) {
                    amount += constraint.getCoefficient(variable).toFloat()
                } else {
                    lastVariable[singleUsedFuel] = variable
                }
                constraint.setCoefficient(variable, amount.toDouble())
            }

            for (ingredient in recipe.ingredients) {
                val variable = variables[ingredient.goods] // TODO split cost analysis
                // was setCoefficientCheck()
                var amount = -ingredient.amount
                if (lastVariable[ingredient.goods] == variable) {
                    amount += constraint.getCoefficient(variable).toFloat()
                } else {
                    lastVariable[ingredient.goods] = variable
                }
                constraint.setCoefficient(variable, amount.toDouble())

                if (ingredient.goods is Item) {
                    logisticsCost += ingredient.amount * CostPerItem
                } else if (ingredient.goods is Fluid) {
                    logisticsCost += ingredient.amount * CostPerFluid
                }
            }

            val sourceEntity = recipe.sourceEntity
            if (sourceEntity != null && sourceEntity.mapGenerated) {
                var totalMining = 0f
                for (product in recipe.products) {
                    totalMining += product.amount
                }
                var miningPenalty = MiningPenalty
                val totalDensity = sourceEntity.mapGenDensity / totalMining
                if (totalDensity < MiningMaxDensityForPenalty) {
                    val extraPenalty = ln(MiningMaxDensityForPenalty / totalDensity)
                    miningPenalty += min(extraPenalty, MiningMaxExtraPenaltyForRarity)
                }

                logisticsCost *= miningPenalty
            }

            if (minEmissions >= 0f) {
                logisticsCost += minEmissions * CostPerPollution * recipe.time
            }

            constraint.setUb(logisticsCost.toDouble())
            cost[recipe] = logisticsCost
            recipeCost[recipe] = logisticsCost
        }

        // TODO this is temporary fix for strange item sources (make the cost of item not higher than the cost of its source)
        for (item in db.items.all) {
            if (shouldInclude(item)) {
                for (source in item.miscSources) {
                    if (source is Goods && shouldInclude(source)) {
                        val constraint =
                            solver.makeConstraint(Double.NEGATIVE_INFINITY, 0.0, "source-" + item.locName)
                        constraint.setCoefficient(variables[source], -1.0)
                        constraint.setCoefficient(variables[item], 1.0)
                    }
                }
            }
        }

        // TODO this is temporary fix for fluid temperatures (make the cost of fluid with lower temp not higher than the cost of fluid with higher temp)
        for ((name, fluids) in db.fluidVariants) {
            var prev = fluids[0]
            for (i in 1 until fluids.size) {
                val cur = fluids[i]
                val constraint =
                    solver.makeConstraint(Double.NEGATIVE_INFINITY, 0.0, "fluid-" + name + "-" + prev.temperature)
                constraint.setCoefficient(variables[prev], 1.0)
                constraint.setCoefficient(variables[cur], -1.0)
                prev = cur
            }
        }

        val result = DataUtils.trySolveWithDifferentSeeds(solver)
        val elapsedTime = System.currentTimeMillis() - time
        logger.info("Cost analysis completed in $elapsedTime ms. with result $result")
        var sumImportance = 1.0
        var totalRecipes = 0
        if (result == MPSolver.ResultStatus.OPTIMAL || result == MPSolver.ResultStatus.FEASIBLE) {
            val objectiveValue = objective.value()
            logger.info(
                "Estimated modpack cost: " + DataUtils.formatAmount(
                    objectiveValue.toFloat() * 1000f,
                    UnitOfMeasure.NONE
                )
            )
            for (g in db.goods.all) {
                val variable = variables[g] ?: continue
                val value = variable.solutionValue()
                cost[g] = value.toFloat()
            }

            for (recipe in db.recipes.all) {
                val constraint = constraints[recipe] ?: continue
                val recipeFlow = constraint.dualValue().toFloat()
                if (recipeFlow > 0f) {
                    totalRecipes += 1
                    sumImportance += recipeFlow
                    flow[recipe] = recipeFlow
                    for (product in recipe.products) {
                        flow[product.goods] = (flow[product.goods] ?: 0f) + (recipeFlow * product.amount)
                    }
                }
            }
        }
        for (o in db.objects.all) {
            if (!shouldInclude(o)) {
                cost[o] = Float.POSITIVE_INFINITY
                continue
            }

            if (o is RecipeOrTechnology) {
                for (ingredient in o.ingredients) { // TODO split
                    cost[o] = (cost[o] ?: 0f) + ((cost[ingredient.goods] ?: 0f) * ingredient.amount)
                }
                for (product in o.products) {
                    recipeProductionCost[o] =
                        (recipeProductionCost[o] ?: 0f) + (product.amount * (cost[product.goods] ?: 0f))
                }
            } else if (o is Entity) {
                var minimal = Float.POSITIVE_INFINITY
                for (item in o.itemsToPlace) {
                    val itemCost = cost[item] ?: 0f
                    if (itemCost < minimal) {
                        minimal = itemCost
                    }
                }
                cost[o] = minimal
            }
        }

        if (result == MPSolver.ResultStatus.OPTIMAL || result == MPSolver.ResultStatus.FEASIBLE) {
            for ((recipe, constraint) in constraints) {
                if (constraint == null) {
                    continue
                }
                var productCost = 0f
                for (product in recipe.products) {
                    productCost += product.amount * (cost[product.goods] ?: 0f)
                }
                recipeWastePercentage[recipe] = 1f - productCost / (cost[recipe] ?: 0f)
            }
        } else {
            if (!onlyCurrentMilestones) {
                errorCollector.appendError(
                    "Cost analysis was unable to process this modpack. This may mean YAFC bug.",
                    ErrorSeverity.ANALYSIS_WARNING
                )
            }
        }

        importantItems.addAll(
            db.goods.all
                .filter { it.usages.size > 1 }
                .sortedByDescending { x ->
                    (flow[x] ?: 0f) * (cost[x]
                        ?: 0f) * x.usages.count { y -> shouldInclude(y) && recipeWastePercentage[y] == 0f }
                }
                .toList()
        )

        solver.delete()
    }

    override val description =
        "Cost analysis computes a hypothetical late-game base. This simulation has two very important results: How much does stuff (items, recipes, etc) cost and how much of stuff do you need. " +
                "It also collects a bunch of auxilary results, for example how efficient are different recipes. These results are used as heuristics and weights for calculations, and are also useful by themselves."

//    fun getDisplayCost(goods: FactorioObject): String {
//        val sb = StringBuilder()
//        var cost = goods.cost()
//        var costNow = goods.Cost(true);
//        if (float.IsPositiveInfinity(cost))
//            return "YAFC analysis: Unable to find a way to fully automate this";
//
//        sb.Clear();
//
//        var compareCost = cost;
//        var compareCostNow = costNow;
//        string costPrefix;
//        if (goods is Fluid)
//        {
//            compareCost = cost * 50;
//            compareCostNow = costNow * 50;
//            costPrefix = "YAFC cost per 50 units of fluid:";
//        }
//        else if (goods is Item)
//            costPrefix = "YAFC cost per item:";
//        else if (goods is Special special && special.isPower)
//        costPrefix = "YAFC cost per 1 MW:";
//        else if (goods is Recipe)
//        costPrefix = "YAFC cost per recipe:";
//    else costPrefix = "YAFC cost:";
//
//        sb.Append(costPrefix).Append(" ¥").Append(DataUtils.FormatAmount(compareCost, UnitOfMeasure.None));
//        if (compareCostNow > compareCost && !float.IsPositiveInfinity(compareCostNow))
//            sb.Append(" (Currently ¥").Append(DataUtils.FormatAmount(compareCostNow, UnitOfMeasure.None)).Append(")");
//        return sb.ToString();
//    }

    fun getBuildingHours(recipe: Recipe, flow: Float): Float {
        return recipe.time * flow * (1000f / 3600f)
    }

    fun fetItemAmount(goods: Goods): String? {
        val itemFlow = flow[goods] ?: 0f
        if (itemFlow <= 1f) {
            return null
        }
        return DataUtils.formatAmount(
            itemFlow * 1000f,
            UnitOfMeasure.NONE,
            "Estimated amount for all researches: "
        )
    }

    companion object {
        private const val CostPerSecond = 0.1f
        private const val CostPerMj = 0.1f
        private const val CostPerIngredientPerSize = 0.1f
        private const val CostPerProductPerSize = 0.2f
        private const val CostPerItem = 0.02f
        private const val CostPerFluid = 0.0005f
        private const val CostPerPollution = 0.01f
        private const val CostLowerLimit = -10.0f
        private const val CostLimitWhenGeneratesOnMap = 1e4
        private const val MiningPenalty = 1f // Penalty for any mining
        private const val MiningMaxDensityForPenalty =
            2000f // Mining things with less density than this gets extra penalty
        private const val MiningMaxExtraPenaltyForRarity = 10f

        private val logger = Logger.getInstance(FactorioCostAnalysis::class.java)
    }
}
