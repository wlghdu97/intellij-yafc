package com.xhlab.yafc.parser.data.deserializer

import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.util.toSet
import com.xhlab.yafc.parser.data.SpecialNames
import com.xhlab.yafc.parser.data.deserializer.FactorioDataDeserializer.TypeWithName.Companion.typeWithName
import java.util.*

class ContextDeserializer constructor(private val parent: FactorioDataDeserializer) {
    private val electricity: MutableSpecial = createSpecialObject(
        isPower = true,
        name = SpecialNames.electricity,
        locName = "Electricity",
        locDescr = "This is an object that represents electric energy",
        icon = FactorioIconPart("__core__/graphics/icons/alerts/electricity-icon-unplugged.png"),
        signal = "signal-E"
    )
    private val heat: MutableSpecial = createSpecialObject(
        isPower = true,
        name = SpecialNames.heat,
        locName = "Heat",
        locDescr = "This is an object that represents heat energy",
        icon = FactorioIconPart("__core__/graphics/arrows/heat-exchange-indication.png"),
        signal = "signal-H"
    )
    private val voidEnergy: MutableSpecial = createSpecialObject(
        isPower = true,
        name = SpecialNames.void,
        locName = "Void",
        locDescr = "This is an object that represents infinite energy",
        icon = FactorioIconPart("__core__/graphics/icons/mip/infinity.png"),
        signal = "signal-V"
    )
    internal val rocketLaunch: MutableSpecial = createSpecialObject(
        isPower = false,
        name = SpecialNames.rocketLaunch,
        locName = "Rocket launch slot",
        locDescr = "This is a slot in a rocket ready to be launched",
        icon = FactorioIconPart("__base__/graphics/entity/rocket-silo/02-rocket.png"),
        signal = "signal-R"
    )

    internal val voidEntityEnergy = MutableEntityEnergy(
        type = EntityEnergyType.VOID,
        effectivity = Float.POSITIVE_INFINITY
    )
    internal val laborEntityEnergy = MutableEntityEnergy(
        type = EntityEnergyType.LABOR,
        effectivity = Float.POSITIVE_INFINITY
    )

    private val generatorProduction: MutableRecipe = createSpecialRecipe(
        production = electricity,
        category = SpecialNames.generatorRecipe,
        hint = "generating",
        products = listOf(MutableProduct(electricity, 1f)),
        flags = RecipeFlag.SCALE_PRODUCTION_WITH_POWER.toSet()
    )

    internal var character: MutableEntity? = null

    init {
        registerSpecial()

        // reactor production
        createSpecialRecipe(
            production = heat,
            category = SpecialNames.reactorRecipe,
            hint = "generating",
            products = listOf(MutableProduct(heat, 1f)),
            flags = RecipeFlag.SCALE_PRODUCTION_WITH_POWER.toSet()
        )
    }

    private fun createSpecialObject(
        isPower: Boolean,
        name: String,
        locName: String,
        locDescr: String,
        icon: FactorioIconPart,
        signal: String
    ): MutableSpecial {
        return parent.getObject(name) {
            MutableSpecial(it, signal, isPower).apply {
                this.factorioType = "special"
                this.locName = locName
                this.locDescr = locDescr
                this.iconSpec = listOf(icon)
                this.fuelValue = if (isPower) 1f else 0f
            }
        }
    }

    private fun registerSpecial() {
        parent.fuels.add(SpecialNames.electricity, electricity)
        parent.fuels.add(SpecialNames.heat, heat)
        parent.fuels.add(SpecialNames.void, voidEnergy)
        parent.rootAccessible.add(voidEnergy)
    }

    private fun skip(from: Int, sortOrder: FactorioObjectSortOrder): Int {
        for (idx in from until parent.allObjects.size) {
            if (parent.allObjects[idx].sortingOrder != sortOrder) {
                return idx
            }
        }

        return parent.allObjects.size
    }

    internal fun exportBuiltData(): YAFCDatabase = with(parent) {
//        allObjects.forEach {
//            it.typeDotName = "${it.type}.${it.name}"
//        }

        val objectsByTypeName = allObjects.associateBy { it.typeDotName }.toMutableMap()
        for (alias in formerAliases) {
            objectsByTypeName[alias.key] = alias.value
        }

        val firstSpecial = 0
        val firstItem = skip(firstSpecial, FactorioObjectSortOrder.SPECIAL_GOODS)
        val firstFluid = skip(firstItem, FactorioObjectSortOrder.ITEMS)
        val firstRecipe = skip(firstFluid, FactorioObjectSortOrder.FLUIDS)
        val firstMechanics = skip(firstRecipe, FactorioObjectSortOrder.RECIPES)
        val firstTechnology = skip(firstMechanics, FactorioObjectSortOrder.MECHANICS)
        val firstEntity = skip(firstTechnology, FactorioObjectSortOrder.TECHNOLOGIES)
        val last = skip(firstEntity, FactorioObjectSortOrder.ENTITIES)
        if (last != allObjects.size) {
            throw RuntimeException("Something is not right")
        }

        val allObjs = FactorioIdRange<FactorioObject>(0, last, allObjects)
        val specials = FactorioIdRange<Special>(firstSpecial, firstItem, allObjects)
        val items = FactorioIdRange<Item>(firstItem, firstFluid, allObjects)
        val fluids = FactorioIdRange<Fluid>(firstFluid, firstRecipe, allObjects)
        val goods = FactorioIdRange<Goods>(firstSpecial, firstRecipe, allObjects)
        val recipes = FactorioIdRange<Recipe>(firstRecipe, firstTechnology, allObjects)
        val mechanics = FactorioIdRange<Mechanics>(firstMechanics, firstTechnology, allObjects)
        val recipesAndTechnologies = FactorioIdRange<RecipeOrTechnology>(firstRecipe, firstEntity, allObjects)
        val technologies = FactorioIdRange<Technology>(firstTechnology, firstEntity, allObjects)
        val entities = FactorioIdRange<Entity>(firstEntity, last, allObjects)

        return YAFCDatabase(
            rootAccessible = rootAccessible,
            allSciencePacks = sciencePacks.toList(),
            objectsByTypeName = objectsByTypeName,
            fluidVariants = fluidVariants,
            voidEnergy = voidEnergy,
            electricity = electricity,
            electricityGeneration = generatorProduction,
            heat = heat,
            character = requireNotNull(character),
            allCrafters = entities.all.filterIsInstance<EntityCrafter>(),
            allModules = allModules,
            allBeacons = entities.all.filterIsInstance<EntityBeacon>(),
            allBelts = entities.all.filterIsInstance<EntityBelt>(),
            allInserters = entities.all.filterIsInstance<EntityInserter>(),
            allAccumulators = entities.all.filterIsInstance<EntityAccumulator>(),
            allContainers = entities.all.filterIsInstance<EntityContainer>(),
            objects = allObjs,
            goods = goods,
            specials = specials,
            items = items,
            fluids = fluids,
            recipes = recipes,
            mechanics = mechanics,
            recipesAndTechnologies = recipesAndTechnologies,
            technologies = technologies,
            entities = entities
        )
    }

    private fun isBarrelingRecipe(barreling: Recipe, unbarreling: Recipe): Boolean {
        val product = barreling.products[0]
        if (product.probability != 1f) {
            return false
        }

        val barrel = product.goods as? Item ?: return false
        if (unbarreling.ingredients.size != 1) {
            return false
        }

        val ingredient = unbarreling.ingredients[0]
        if (ingredient.variants != null || ingredient.goods != barrel || ingredient.amount != product.amount) {
            return false
        }
        if (unbarreling.products.size != barreling.ingredients.size) {
            return false
        }
        if (barrel.miscSources.isNotEmpty() ||
            barrel.fuelValue != 0f ||
            barrel.placeResult != null ||
            barrel.module != null
        ) {
            return false
        }

        for ((testProduct, testIngredient) in unbarreling.products.zip(barreling.ingredients)) {
            if (testProduct.probability != 1f ||
                testProduct.goods != testIngredient.goods ||
                testIngredient.variants != null ||
                testProduct.amount != testIngredient.amount
            ) {
                return false
            }
        }

        if (unbarreling.isProductivityAllowed() || barreling.isProductivityAllowed()) {
            return false
        }

        return true
    }

    internal fun calculateMaps() {
        val itemUsages = DataBucket<MutableGoods, MutableRecipe>()
        val itemProduction = DataBucket<MutableGoods, MutableRecipe>()
        val miscSources = DataBucket<MutableGoods, MutableFactorioObject>()
        val entityPlacers = DataBucket<MutableEntity, MutableItem>()
        val recipeUnlockers = DataBucket<MutableRecipe, MutableTechnology>()
        // Because actual recipe availability may be different from just "all recipes from that category" because of item slot limit and fluid usage restriction, calculate it here
        val actualRecipeCrafters = DataBucket<MutableRecipeOrTechnology, MutableEntityCrafter>()
        val usageAsFuel = DataBucket<MutableGoods, MutableEntity>()
        val allRecipes = arrayListOf<MutableRecipe>()
        val allMechanics = arrayListOf<MutableMechanics>()

        // step 1 - collect maps

        for (idx in 0 until parent.allObjects.size) {
            when (val o = parent.allObjects[idx]) {
                is MutableTechnology -> {
                    for (recipe in o.unlockRecipes) {
                        recipeUnlockers.add(recipe, o)
                    }
                }

                is MutableRecipe -> {
                    allRecipes.add(o)

                    for (product in o.products) {
                        if (product.amount > 0) {
                            itemProduction.add(product.goods, o)
                        }
                    }

                    for (ingredient in o.ingredients) {
                        val variants = ingredient.variants
                        if (variants == null) {
                            itemUsages.add(ingredient.goods, o)
                        } else {
                            ingredient.goods = variants[0]
                            for (variant in variants) {
                                itemUsages.add(variant, o)
                            }
                        }
                    }

                    if (o is MutableMechanics) {
                        allMechanics.add(o)
                    }
                }

                is MutableItem -> {
                    val placeResultStr = parent.placeResults[o]
                    if (placeResultStr != null) {
                        val placeResult = parent
                            .getObjectWithNominal<MutableEntity, MutableEntity>(placeResultStr, ::MutableEntityImpl)
                        o.placeResult = placeResult
                        entityPlacers.add(placeResult, o)
                    }

                    val fuelResult = o.fuelResult
                    if (fuelResult != null) {
                        miscSources.add(fuelResult, o)
                    }
                }

                is MutableEntity -> {
                    for (product in o.loot) {
                        miscSources.add(product.goods, o)
                    }

                    if (o is MutableEntityCrafter) {
                        o.recipes = parent.recipeCrafters.getRaw(o).flatMap {
                            parent.recipeCategories.getRaw(it).filter { category ->
                                category.canFit(o.itemInputs, o.fluidInputs, o.inputs)
                            }
                        }
                        o.recipes.forEach { recipe ->
                            actualRecipeCrafters.add(recipe, o, true)
                        }
                    }

                    val energy = o.energy
                    if (energy != null && o.energy != voidEntityEnergy) {
                        var fuelList = parent.fuelUsers.getRaw(o).flatMap { parent.fuels.getRaw(it) }
                        if (energy.type == EntityEnergyType.FLUID_HEAT) {
                            fuelList = fuelList.asSequence().filterIsInstance<MutableFluid>().filter {
                                energy.acceptedTemperature.contains(it.temperature) && it.temperature > energy.workingTemperature.min
                            }.toList()
                        }

                        o.energy?.fuels = fuelList

                        for (fuel in fuelList) {
                            usageAsFuel.add(fuel, o)
                        }
                    }
                }

                else -> Unit
            }
        }

        voidEntityEnergy.fuels = listOf(parent.context.voidEnergy)

        actualRecipeCrafters.sealAndDeduplicate()
        usageAsFuel.sealAndDeduplicate()
        recipeUnlockers.sealAndDeduplicate()
        entityPlacers.sealAndDeduplicate()

        // step 2 - fill maps

        for (idx in 0 until parent.allObjects.size) {
            when (val o = parent.allObjects[idx]) {
                is MutableRecipeOrTechnology -> {
                    if (o is MutableRecipe) {
                        o.fallbackLocalization(o.mainProduct, "A recipe to create")
                        o.technologyUnlock = recipeUnlockers.getList(o)
                    }
                    o.crafters = actualRecipeCrafters.getList(o)
                }

                is MutableGoods -> {
                    o.usages = itemUsages.getList(o)
                    o.production = itemProduction.getList(o)
                    o.miscSources = miscSources.getList(o)

                    if (o is MutableItem) {
                        if (o.placeResult != null) {
                            o.fallbackLocalization(o.placeResult, "An item to build")
                        }
                    } else if (o is MutableFluid && o.variants.isNotEmpty()) {
                        val temperatureDescr = "Temperature: " + o.temperature + "°"
                        if (o.locDescr.isEmpty()) {
                            o.locDescr = temperatureDescr
                        } else {
                            o.locDescr = temperatureDescr + "\n" + o.locDescr
                        }
                    }

                    o.fuelFor = usageAsFuel.getList(o)
                }

                is MutableEntity -> {
                    o.itemsToPlace = entityPlacers.getList(o)
                }
            }
        }

        for (mechanic in allMechanics) {
            mechanic.locName = mechanic.source.locName + " " + mechanic.locName
            mechanic.locDescr = mechanic.source.locDescr
            mechanic.iconSpec = mechanic.source.iconSpec
        }

        // step 3 - detect barreling/unbarreling and voiding recipes
        for (recipe in allRecipes) {
            if (recipe.specialType != FactorioObjectSpecialType.NORMAL) {
                continue
            }
            if (recipe.products.isEmpty()) {
                recipe.specialType = FactorioObjectSpecialType.VOIDING
                continue
            }
            if (recipe.products.size != 1 || recipe.ingredients.isEmpty()) {
                continue
            }

            val barrel = recipe.products[0].goods as? MutableItem
            if (barrel != null) {
                for (usage in barrel.usages) {
                    if (isBarrelingRecipe(recipe, usage)) {
                        recipe.specialType = FactorioObjectSpecialType.BARRELING
                        usage.specialType = FactorioObjectSpecialType.UNBARRELING
                        barrel.specialType = FactorioObjectSpecialType.FILLED_BARREL
                    }
                }
            }
        }

        for (idx in 0 until parent.allObjects.size) {
            val any = parent.allObjects[idx]
            if (any.locName.isEmpty()) {
                any.locName = any.name
            }
        }

        for (list in parent.fluidVariants.values) {
            for (fluid in list) {
                fluid.locName += " " + fluid.temperature + "°"
            }
        }
    }

    internal fun createSpecialRecipe(
        production: MutableFactorioObject,
        category: String,
        hint: String,
        products: List<MutableProduct> = emptyList(),
        ingredients: List<MutableIngredient> = emptyList(),
        flags: RecipeFlags = EnumSet.noneOf(RecipeFlag::class.java)
    ): MutableRecipe {
        val fullName = "$category${(if (category.endsWith(".")) "" else ".")}${production.name}"

        val recipeRaw = parent.registeredObjects[typeWithName<MutableMechanics>(fullName)] as? MutableMechanics
        if (recipeRaw != null) {
            return recipeRaw
        }

        val recipe = parent.getObject(fullName) {
            MutableMechanics(production, fullName).apply {
                this.factorioType = SpecialNames.fakeRecipe
                this.locName = hint
                this.ingredients = ingredients
                this.products = products
                this.time = 1f
                this.enabled = true
                this.hidden = true
                this.flags = flags
            }
        }

        parent.recipeCategories.add(category, recipe)

        return recipe
    }
}
