package com.xhlab.yafc.parser.data.deserializer

import com.xhlab.yafc.model.Version
import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.parser.data.SpecialNames
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import kotlin.math.*

internal class EntityDeserializer constructor(
    private val parent: FactorioDataDeserializer,
    private val factorioVersion: Version
) {
    private fun getFluidBoxFilter(
        table: LuaTable,
        fluidBoxName: String,
        temperature: Int
    ): Triple<Boolean, MutableFluid?, TemperatureRange> {
        val fluidBoxData = table[fluidBoxName].opttable(null)
            ?: return Triple(false, null, TemperatureRange.Any)
        val fluidName = fluidBoxData["filter"].optjstring(null)
            ?: return Triple(false, null, TemperatureRange.Any)

        val fluid = if (temperature == 0) {
            parent.getObject(fluidName, ::MutableFluid)
        } else {
            parent.common.getFluidFixedTemp(fluidName, temperature)
        }

        val min = fluidBoxData["minimum_temperature"].optint(fluid.temperatureRange.min)
        val max = fluidBoxData["maximum_temperature"].optint(fluid.temperatureRange.max)

        return Triple(true, fluid, TemperatureRange(min, max))
    }

    private fun countFluidBoxes(list: LuaTable, input: Boolean): Int {
        var count = 0
        list.keys().mapNotNull { if (list[it].istable()) list[it].checktable() else null }.forEach { fluidBox ->
            val prodType = fluidBox["production_type"].optjstring(null)
            if (prodType != null && (prodType == "input-output" || input && prodType == "input" || !input && prodType == "output")) {
                count += 1
            }
        }

        return count
    }

    private fun readFluidEnergySource(energySource: LuaTable, entity: MutableEntity) {
        val energy = entity.energy ?: return

        val burns = energySource["burns_fluid"].optboolean(false)
        energy.type = if (burns) EntityEnergyType.FLUID_FUEL else EntityEnergyType.FLUID_HEAT

        energy.workingTemperature = TemperatureRange.Any
        val fuelLimit = energySource["fluid_usage_per_tick"].optdouble(Double.NaN).toFloat()
        if (!fuelLimit.isNaN()) {
            energy.fuelConsumptionLimit = fuelLimit * 60f
        }

        val (hasFilter, fluid, filterTemperature) = getFluidBoxFilter(energySource, "fluid_box", 0)
        if (hasFilter && fluid != null) {
            val fuelCategory = SpecialNames.specificFluid + fluid.name
            parent.fuelUsers.add(entity, fuelCategory)
            if (!burns) {
                val temperature = fluid.temperatureRange
                val maxT = energySource["maximum_temperature"].optint(Int.MAX_VALUE)
                energy.workingTemperature = temperature.copy(max = min(temperature.max, maxT))
                energy.acceptedTemperature = filterTemperature
            }
        } else if (burns) {
            parent.fuelUsers.add(entity, SpecialNames.burnableFluid)
        } else {
            parent.fuelUsers.add(entity, SpecialNames.hotFluid)
        }
    }

    private fun readEnergySource(energySource: LuaTable, entity: MutableEntity, defaultDrain: Float = 0f) {
        val type = energySource["type"].optjstring("burner")

        if (type == "void") {
            entity.energy = parent.context.voidEntityEnergy
            return
        }

        val energy = MutableEntityEnergy()
        entity.energy = energy
        energy.emissions = energySource["emissions_per_minute"].optdouble(0.0).toFloat()
        energy.effectivity = energySource["effectivity"].optdouble(1.0).toFloat()

        when (type) {
            "electric" -> {
                parent.fuelUsers.add(entity, SpecialNames.electricity)
                energy.type = EntityEnergyType.ELECTRIC
                val drainS = energySource["drain"].optjstring(null)
                energy.drain = if (drainS == null) defaultDrain else parent.common.parseEnergy(drainS)
            }

            "burner" -> {
                energy.type = EntityEnergyType.SOLID_FUEL
                val categories = energySource["fuel_categories"].opttable(null)
                if (categories != null) {
                    categories.keys().mapNotNull { categories[it].optjstring(null) }.forEach {
                        parent.fuelUsers.add(entity, it)
                    }
                } else {
                    parent.fuelUsers.add(entity, energySource["fuel_category"].optjstring("chemical"))
                }
            }

            "heat" -> {
                energy.type = EntityEnergyType.HEAT
                parent.fuelUsers.add(entity, SpecialNames.heat)
                energy.workingTemperature = TemperatureRange(
                    energySource["min_working_temperature"].optint(15),
                    energySource["max_temperature"].optint(15)
                )
            }

            "fluid" -> {
                readFluidEnergySource(energySource, entity)
            }
        }
    }

    private fun getSize(box: LuaTable): Int {
        val topLeft = box[1].checktable()
        val bottomRight = box[2].checktable()

        val x0 = topLeft[1].todouble().toFloat()
        val y0 = topLeft[2].todouble().toFloat()
        val x1 = bottomRight[1].todouble().toFloat()
        val y1 = bottomRight[2].todouble().toFloat()

        return max(round(x1 - x0), round(y1 - y0)).toInt()
    }

    private fun parseModules(table: LuaTable, entity: MutableEntityWithModules, def: AllowedEffects) {
        val allowedEffects = table["allowed_effects"]
        if (!allowedEffects.isnil()) {
            if (allowedEffects.isstring()) {
                entity.allowedEffects = AllowedEffects.fromString(allowedEffects.tojstring())
            } else if (allowedEffects.istable()) {
                entity.allowedEffects = AllowedEffects.NONE
                val effectsTable = allowedEffects.checktable()
                effectsTable.keys().mapNotNull { effectsTable[it].optjstring(null) }.forEach {
                    val effects = AllowedEffects.fromString(it)
                    entity.allowedEffects = entity.allowedEffects or effects
                }
            }
        } else {
            entity.allowedEffects = def
        }

        val moduleSpec = table["module_specification"].opttable(null)
        if (moduleSpec != null) {
            entity.moduleSlots = moduleSpec["module_slots"].optint(0)
        }
    }

    private fun createLaunchRecipe(
        entity: MutableEntityCrafter,
        recipe: MutableRecipe,
        partsRequired: Int,
        outputCount: Int
    ): MutableRecipe {
        val launchCategory = SpecialNames.rocketCraft + entity.name
        val launchRecipe = parent.context.createSpecialRecipe(recipe, launchCategory, "launch")
        parent.recipeCrafters.add(entity, launchCategory)
        launchRecipe.ingredients = recipe.products.map { MutableIngredient(it.goods, it.amount * partsRequired) }
        launchRecipe.products = listOf(MutableProduct(parent.context.rocketLaunch, outputCount.toFloat()))
        launchRecipe.time = 40.33f / outputCount
        parent.recipeCrafters.add(entity, SpecialNames.rocketLaunch)

        return launchRecipe
    }

    internal val rocketEntitiesDeserializer = object : CommonDeserializer.Deserializer {
        override fun deserialize(table: LuaTable) {
            table.keys().mapNotNull { table[it].opttable(null) }.forEach {
                val rocket = it.opttable(null)
                if (rocket != null) {
                    val inventorySize = rocket["inventory_size"].optint(1)
                    parent.rocketInventorySizes[rocket["name"].optjstring("")] = inventorySize
                }
            }
        }
    }

    internal val entityDeserializer = object : CommonDeserializer.Deserializer {
        override fun deserialize(table: LuaTable) {
            val factorioType = table["type"].tojstring()
            val name = table["name"].tojstring()

            var defaultDrain = 0f
            when (factorioType) {
                "transport-belt" -> {
                    parent.getObjectWithNominal<MutableEntity, MutableEntityBelt>(name, ::MutableEntityBelt).apply {
                        beltItemsPerSecond = table["speed"].optdouble(0.0).toFloat() * 480f
                    }
                }

                "inserter" -> {
                    parent.getObjectWithNominal<MutableEntity, MutableEntityInserter>(name, ::MutableEntityInserter)
                        .apply {
                            inserterSwingTime = 1f / (table["rotation_speed"].optdouble(1.0).toFloat() * 60)
                            isStackInserter = table["stack"].optboolean(false)
                        }
                }

                "accumulator" -> {
                    val accumulator = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityAccumulator>(name, ::MutableEntityAccumulator)
                    val accumulatorEnergy = table["energy_source"].opttable(null)
                    if (accumulatorEnergy != null) {
                        val capacity = accumulatorEnergy["buffer_capacity"].optjstring(null)
                        if (capacity != null) {
                            accumulator.accumulatorCapacity = parent.common.parseEnergy(capacity)
                        }
                    }
                }

                "reactor" -> {
                    val reactor =
                        parent.getObjectWithNominal<MutableEntity, MutableEntityReactor>(name, ::MutableEntityReactor)
                            .apply {
                                reactorNeighbourBonus = table["neighbour_bonus"].optdouble(1.0).toFloat()
                                val usesPower = table["consumption"].tojstring()
                                power = parent.common.parseEnergy(usesPower)
                                craftingSpeed = power
                            }

                    parent.recipeCrafters.add(reactor, SpecialNames.reactorRecipe)
                }

                "beacon" -> {
                    parent.getObjectWithNominal<MutableEntity, MutableEntityBeacon>(name, ::MutableEntityBeacon).apply {
                        beaconEfficiency = table["distribution_effectivity"].optdouble(0.0).toFloat()
                        val usesPower = table["energy_usage"].tojstring()
                        parseModules(table, this, AllowedEffects.ALL xor AllowedEffects.PRODUCTIVITY)
                        power = parent.common.parseEnergy(usesPower)
                    }
                }

                "logistic-container",
                "container" -> {
                    parent.getObjectWithNominal<MutableEntity, MutableEntityContainer>(name, ::MutableEntityContainer)
                        .apply {
                            inventorySize = table["inventory_size"].optint(0)
                            if (factorioType == "logistic-container") {
                                logisticMode = table["logistic_mode"].optjstring("")
                                logisticSlotsCount = table["logistic_slots_count"].optint(0)
                                if (logisticSlotsCount == 0) {
                                    logisticSlotsCount = table["max_logistic_slots"].optint(1000)
                                }
                            }
                        }
                }

                "character" -> {
                    val character = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)
                    character.itemInputs = 255

                    val miningCategories = table["mining_categories"].opttable(null)
                    miningCategories?.keys()?.mapNotNull { miningCategories[it].optjstring(null) }?.forEach {
                        parent.recipeCrafters.add(character, SpecialNames.miningRecipe + it)
                    }

                    val craftingCategories = table["crafting_categories"].opttable(null)
                    craftingCategories?.keys()?.mapNotNull { craftingCategories[it].optjstring(null) }?.forEach {
                        parent.recipeCrafters.add(character, it)
                    }

                    character.energy = parent.context.laborEntityEnergy
                    if (character.name == "character") {
                        parent.context.character = character
                        character.mapGenerated = true
                        parent.rootAccessible.add(0, character)
                    }
                }

                "boiler" -> {
                    val boiler = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)

                    val usesPower = table["energy_consumption"].tojstring()
                    boiler.power = parent.common.parseEnergy(usesPower)
                    boiler.fluidInputs = 1

                    val hasOutput = table["mode"].optjstring("") == "output-to-separate-pipe"
                    val (_, input, acceptTemperature) = getFluidBoxFilter(table, "fluid_box", 0)
                    val targetTemp = table["target_temperature"].toint()

                    val output = if (hasOutput) {
                        getFluidBoxFilter(table, "output_fluid_box", targetTemp).second
                    } else {
                        input
                    }

                    if (input != null && output != null) { // TODO - boiler works with any fluid - not supported
                        // otherwise convert boiler production to a recipe
                        val category = SpecialNames.boilerRecipe + boiler.name
                        val recipe = parent.context.createSpecialRecipe(output, category, "boiling to $targetTemp°")
                        parent.recipeCrafters.add(boiler, category)
                        recipe.flags = RecipeFlags.USES_FLUID_TEMPERATURE or recipe.flags
                        recipe.ingredients = listOf(
                            MutableIngredient(input, 60f).apply {
                                temperature = acceptTemperature
                            }
                        )
                        recipe.products = listOf(MutableProduct(output, 60f))
                        // This doesn't mean anything as RecipeFlags.UsesFluidTemperature overrides recipe time, but looks nice in the tooltip
                        recipe.time = input.heatCapacity * 60 *
                                (output.temperature - max(input.temperature, input.temperatureRange.min)) / boiler.power
                        boiler.craftingSpeed = 1f / boiler.power
                    }
                }

                "assembling-machine",
                "rocket-silo",
                "furnace" -> {
                    val crafter = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)

                    val usesPower = table["energy_usage"].tojstring()
                    parseModules(table, crafter, AllowedEffects.NONE)
                    crafter.power = parent.common.parseEnergy(usesPower)
                    defaultDrain = crafter.power / 30f
                    crafter.craftingSpeed = table["crafting_speed"].optdouble(1.0).toFloat()
                    crafter.itemInputs = if (factorioType == "furnace") {
                        table["source_inventory_size"].optint(1)
                    } else {
                        table["ingredient_count"].optint(255)
                    }

                    val fluidBoxes = table["fluid_boxes"].opttable(null)
                    if (fluidBoxes != null) {
                        crafter.fluidInputs = countFluidBoxes(fluidBoxes, true)
                    }

                    var fixedRecipe: MutableRecipeImpl? = null
                    val fixedRecipeName = table["fixed_recipe"].optjstring(null)
                    if (fixedRecipeName != null) {
                        val fixedRecipeCategoryName = SpecialNames.fixedRecipe + fixedRecipeName
                        val recipe = parent.getObject(fixedRecipeName, ::MutableRecipeImpl)
                        parent.recipeCrafters.add(crafter, fixedRecipeCategoryName)
                        parent.recipeCategories.add(fixedRecipeCategoryName, recipe)
                        fixedRecipe = recipe
                    } else {
                        val craftingCategories = table["crafting_categories"].opttable(LuaValue.tableOf())
                        craftingCategories.keys().mapNotNull { craftingCategories[it].optjstring(null) }.forEach {
                            parent.recipeCrafters.add(crafter, it)
                        }
                    }

                    if (factorioType == "rocket-silo") {
                        val resultInventorySize = table["rocket_result_inventory_size"].optint(1)
                        if (resultInventorySize > 0) {
                            val rocketEntry = table["rocket_entity"].optjstring(null)
                            val outputCount = rocketEntry?.let { parent.rocketInventorySizes[it] } ?: 1
                            val partsRequired = table["rocket_parts_required"].optint(100)
                            if (fixedRecipe != null) {
                                val launchRecipe = createLaunchRecipe(crafter, fixedRecipe, partsRequired, outputCount)
                                parent.formerAliases["Mechanics.launch" + crafter.name + "." + crafter.name] =
                                    launchRecipe
                            } else {
                                parent.recipeCrafters.getRaw(crafter).forEach { categoryName ->
                                    parent.recipeCategories.getRaw(categoryName).forEach { possibleRecipe ->
                                        if (possibleRecipe is MutableRecipeImpl) {
                                            createLaunchRecipe(crafter, possibleRecipe, partsRequired, outputCount)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "generator",
                "burner-generator" -> {
                    val generator = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)
                    // generator energy input config is strange
                    val maxPowerOutput = table["max_power_output"].optjstring(null)
                    if (maxPowerOutput != null) {
                        generator.power = parent.common.parseEnergy(maxPowerOutput)
                    }

                    val burnerSource = table["burner"].opttable(null)
                    if ((factorioVersion < v0_18 || factorioType == "burner-generator") && burnerSource != null) {
                        readEnergySource(burnerSource, generator)
                    } else {
                        generator.energy =
                            MutableEntityEnergy(effectivity = table["effectivity"].optdouble(1.0).toFloat())
                        readFluidEnergySource(table, generator)
                    }

                    parent.recipeCrafters.add(generator, SpecialNames.generatorRecipe)
                }

                "mining-drill" -> {
                    val drill = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)
                    val usesPower = table["energy_usage"].tojstring()
                    drill.power = parent.common.parseEnergy(usesPower)
                    parseModules(table, drill, AllowedEffects.ALL)
                    drill.craftingSpeed = table["mining_speed"].optdouble(1.0).toFloat()

                    val resourceCategories = table["resource_categories"].opttable(LuaValue.tableOf())
                    if (table["input_fluid_box"].istable()) {
                        drill.fluidInputs = 1
                    }
                    resourceCategories.keys().mapNotNull { resourceCategories[it].optjstring(null) }.forEach {
                        parent.recipeCrafters.add(drill, SpecialNames.miningRecipe + it)
                    }
                }

                "offshore-pump" -> {
                    val pump = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)
                    pump.craftingSpeed = table["pumping_speed"].optdouble(20.0).toFloat() / 20f
                    val fluidName = table["fluid"].tojstring()
                    val pumpingFluid = parent.common.getFluidFixedTemp(fluidName, 0)
                    val recipeCategory = SpecialNames.pumpingRecipe + pumpingFluid.name
                    val recipe = parent.context.createSpecialRecipe(pumpingFluid, recipeCategory, "pumping")
                    parent.recipeCrafters.add(pump, recipeCategory)
                    pump.energy = parent.context.voidEntityEnergy

                    if (recipe.products.isEmpty()) {
                        // set to Factorio default pump amounts - looks nice in tooltip
                        recipe.products = listOf(MutableProduct(pumpingFluid, 1200f))
                        recipe.ingredients = emptyList()
                        recipe.time = 1f
                    }
                }

                "lab" -> {
                    val lab = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)
                    val usesPower = table["energy_usage"].tojstring()
                    parseModules(table, lab, AllowedEffects.ALL)
                    lab.power = parent.common.parseEnergy(usesPower)
                    lab.craftingSpeed = table["researching_speed"].optdouble(1.0).toFloat()
                    parent.recipeCrafters.add(lab, SpecialNames.labs)

                    val inputs = table["inputs"].opttable(LuaValue.tableOf())
                    lab.inputs = inputs.keys().mapNotNull { inputs[it].optjstring(null) }
                        .map { parent.getObject(it, ::MutableItem) }

                    parent.sciencePacks.addAll(lab.inputs.filterIsInstance<MutableItem>())
                    lab.itemInputs = lab.inputs.size
                }

                "solar-panel" -> {
                    val solarPanel = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)
                    solarPanel.energy = parent.context.voidEntityEnergy

                    val powerProduction = table["production"].tojstring()
                    parent.recipeCrafters.add(solarPanel, SpecialNames.generatorRecipe)
                    solarPanel.craftingSpeed =
                        parent.common.parseEnergy(powerProduction) * 0.7f // 0.7f is a solar panel ratio on nauvis
                }

                "electric-energy-interface" -> {
                    val eei = parent
                        .getObjectWithNominal<MutableEntity, MutableEntityCrafterImpl>(name, ::MutableEntityCrafterImpl)
                    eei.energy = parent.context.voidEntityEnergy

                    val interfaceProduction = table["energy_production"].optjstring(null)
                    if (interfaceProduction != null) {
                        eei.craftingSpeed = parent.common.parseEnergy(interfaceProduction)
                        if (eei.craftingSpeed > 0) {
                            parent.recipeCrafters.add(eei, SpecialNames.generatorRecipe)
                        }
                    }
                }

                "constant-combinator" -> {
                    if (name == "constant-combinator") {
                        YAFCDatabase.constantCombinatorCapacity = table["item_slot_count"].optint(18)
                    }
                }
            }

            val entity: MutableEntity = parent.common
                .deserializeCommonWithNominal<MutableEntity, MutableEntity>(table, "entity", ::MutableEntityImpl)

            val lootList = table["loot"].opttable(null)
            if (lootList != null) {
                entity.loot = lootList.keys().mapNotNull { lootList[it].opttable(null) }.map {
                    val goods = parent.getObject(it["item"].tojstring(), ::MutableItem)
                    MutableProduct(
                        goods,
                        it["count_min"].optdouble(1.0).toFloat(),
                        it["count_max"].optdouble(1.0).toFloat(),
                        it["probability"].optdouble(1.0).toFloat()
                    )
                }
            }

            val minable = table["minable"].opttable(null)
            if (minable != null) {
                val products = parent.recipeAndTechnology.loadProductList(minable)
                if (factorioType == "resource") {
                    // mining resource is processed as a recipe
                    val category = table["category"].optjstring("basic-solid")
                    val recipe =
                        parent.context.createSpecialRecipe(entity, SpecialNames.miningRecipe + category, "mining")
                    recipe.flags = RecipeFlags.USES_MINING_PRODUCTIVITY or RecipeFlags.LIMITED_BY_TICK_RATE
                    recipe.time = minable["mining_time"].optdouble(1.0).toFloat()
                    recipe.products = products
                    recipe.modules = parent.allModules
                    recipe.sourceEntity = entity

                    val requiredFluid = minable["required_fluid"].optjstring(null)
                    if (requiredFluid != null) {
                        val amount = minable["fluid_amount"].todouble().toFloat()
                        val fluid = parent.getObject(requiredFluid, ::MutableFluid)
                        recipe.ingredients =
                            listOf(MutableIngredient(fluid, amount / 10f)) // 10x difference is correct but why?
                    } else {
                        recipe.ingredients = emptyList()
                    }
                } else {
                    // otherwise it is processed as loot
                    entity.loot = products
                }
            }

            val box = table["selection_box"].opttable(null)
            entity.size = if (box != null) getSize(box) else 3

            val energySource = table["energy_source"].opttable(null)
            if (factorioType != "generator" && factorioType != "solar-panel" && factorioType != "accumulator" &&
                factorioType != "burner-generator" && factorioType != "offshore-pump" && energySource != null
            ) {
                readEnergySource(energySource, entity, defaultDrain)
            }
            if (entity is MutableEntityCrafter) {
                entity.productivity = table["base_productivity"].optdouble(0.0).toFloat()
            }

            val generation = table["autoplace"].opttable(null)
            if (generation != null) {
                entity.mapGenerated = true
                parent.rootAccessible.add(entity)

                val prob = generation["probability_expression"].opttable(null)
                val coverage = generation["coverage"].optdouble(Double.NaN).toFloat()
                if (prob != null) {
                    val probability = estimateNoiseExpression(prob)
                    val rich = generation["richness_expression"].opttable(null)
                    val richness = if (rich != null) estimateNoiseExpression(rich) else probability
                    entity.mapGenDensity = richness * probability
                } else if (!coverage.isNaN()) {
                    val richBase = generation["richness_base"].optdouble(0.0).toFloat()
                    val richMult = generation["richness_multiplier"].optdouble(0.0).toFloat()
                    val richMultDist = generation["richness_multiplier_distance_bonus"].optdouble(0.0).toFloat()
                    val estimatedAmount =
                        coverage * (richBase + richMult + richMultDist * ESTIMATION_DISTANCE_FROM_CENTER)
                    entity.mapGenDensity = estimatedAmount
                }
            }

            if (entity.energy == parent.context.voidEntityEnergy ||
                entity.energy == parent.context.laborEntityEnergy
            ) {
                parent.fuelUsers.add(entity, SpecialNames.void)
            }
        }
    }

    private fun estimateArgument(args: LuaTable, name: String, def: Float = 0f): Float {
        val res = args[name].opttable(null)
        return if (res != null) estimateNoiseExpression(res) else def
    }

    private fun estimateArgument(args: LuaTable, index: Int, def: Float = 0f): Float {
        val res = args[index].opttable(null)
        return if (res != null) estimateNoiseExpression(res) else def
    }

    private fun estimateNoiseExpression(expression: LuaTable): Float {
        when (expression["type"].optjstring("typed")) {
            "variable" -> {
                val varName = expression["variable_name"].optjstring("")
                if (varName == "x" || varName == "y" || varName == "distance") {
                    return ESTIMATION_DISTANCE_FROM_CENTER
                }

                val noiseExpression = parent.common.raw["noise-expression"][varName].opttable(null)
                if (noiseExpression != null) {
                    return estimateArgument(noiseExpression, "expression")
                }

                return 1f
            }

            "function-application" -> {
                val funName = expression["function_name"].optjstring("")
                val args = expression["arguments"].opttable(null)
                when (funName) {
                    "add" -> {
                        var res = 0f
                        args?.keys()?.mapNotNull { args[it].opttable(null) }?.forEach {
                            res += estimateNoiseExpression(it)
                        }

                        return res
                    }

                    "multiply" -> {
                        var res = 1f
                        args?.keys()?.mapNotNull { args[it].opttable(null) }?.forEach {
                            res *= estimateNoiseExpression(it)
                        }

                        return res
                    }

                    "subtract" -> {
                        return estimateArgument(args, 1) - estimateArgument(args, 2)
                    }

                    "divide" -> {
                        return estimateArgument(args, 1) / estimateArgument(args, 2)
                    }

                    "exponentiate" -> {
                        return estimateArgument(args, 1).pow((estimateArgument(args, 2)))
                    }

                    "absolute-value" -> {
                        return abs(estimateArgument(args, 1))
                    }

                    "clamp" -> {
                        return max(estimateArgument(args, 2), min(estimateArgument(args, 3), estimateArgument(args, 1)))
                    }

                    "log2" -> {
                        return log2(estimateArgument(args, 1))
                    }

                    "distance-from-nearest-point" -> {
                        return estimateArgument(args, "maximum_distance")
                    }

                    "ridge" -> {
                        return (estimateArgument(args, 2) + estimateArgument(args, 3)) * 0.5f // TODO
                    }

                    "terrace" -> {
                        return estimateArgument(args, "value") // TODO what terrace does
                    }

                    "random-penalty" -> {
                        val source = estimateArgument(args, "source")
                        val penalty = estimateArgument(args, "amplitude")
                        if (penalty > source) {
                            return source / penalty
                        }

                        return (source + source - penalty) / 2
                    }

                    "spot-noise" -> {
                        val quantity = estimateArgument(args, "spot_quantity_expression")
                        val spots = args["candidate_spot_count"].opttable(null)
                        val spotCount = if (spots != null) {
                            estimateNoiseExpression(spots)
                        } else {
                            estimateArgument(args, "candidate_point_count", 256f) / estimateArgument(
                                args,
                                "skip_span",
                                1f
                            )
                        }

                        val regionSize = estimateArgument(args, "region_size", 512f).pow(2)
                        return spotCount * quantity / regionSize
                    }

                    "factorio-basis-noise",
                    "factorio-quick-multioctave-noise",
                    "factorio-multioctave-noise" -> {
                        val outputScale = estimateArgument(args, "output_scale", 1f)
                        return 0.1f * outputScale
                    }

                    else -> {
                        return 0f
                    }
                }
            }

            "procedure-delimiter" -> {
                return estimateArgument(expression, "expression")
            }

            "literal-number" -> {
                return expression["literal_value"].optdouble(0.0).toFloat()
            }

            "literal-expression" -> {
                return estimateArgument(expression, "literal_value")
            }

            else -> {
                return 0f
            }
        }
    }

    companion object {
        private const val ESTIMATION_DISTANCE_FROM_CENTER = 3000f

        private val v0_18 = Version(0, 18)
    }
}
