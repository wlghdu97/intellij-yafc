package com.xhlab.yafc.parser.data.deserializer

import com.xhlab.yafc.model.Version
import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityEnergyType
import com.xhlab.yafc.parser.data.SpecialNames
import com.xhlab.yafc.parser.data.deserializer.FactorioDataDeserializer.TypeWithName.Companion.typeWithName
import com.xhlab.yafc.parser.data.mutable.*
import com.xhlab.yafc.parser.data.mutable.entity.MutableEntity
import com.xhlab.yafc.parser.data.mutable.entity.MutableEntityEnergy

class ContextDeserializer constructor(
    private val parent: FactorioDataDeserializer,
    private val expensiveRecipes: Boolean,
    private val factorioVersion: Version
) {
    private val electricity: MutableSpecial = createSpecialObject(
        isPower = true,
        name = SpecialNames.electricity,
        locName = "Electricity",
        locDescr = "This is an object that represents electric energy",
        icon = "__core__/graphics/icons/alerts/electricity-icon-unplugged.png",
        signal = "signal-E"
    )
    private val heat: MutableSpecial = createSpecialObject(
        isPower = true,
        name = SpecialNames.heat,
        locName = "Heat",
        locDescr = "This is an object that represents heat energy",
        icon = "__core__/graphics/arrows/heat-exchange-indication.png",
        signal = "signal-H"
    )
    private val voidEnergy: MutableSpecial = createSpecialObject(
        isPower = true,
        name = SpecialNames.void,
        locName = "Void",
        locDescr = "This is an object that represents infinite energy",
        icon = "__core__/graphics/icons/mip/infinity.png",
        signal = "signal-V"
    )
    internal val rocketLaunch: MutableSpecial = createSpecialObject(
        isPower = false,
        name = SpecialNames.rocketLaunch,
        locName = "Rocket launch slot",
        locDescr = "This is a slot in a rocket ready to be launched",
        icon = "__base__/graphics/entity/rocket-silo/02-rocket.png",
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
        flags = RecipeFlags.SCALE_PRODUCTION_WITH_POWER
    )
    private val reactorProduction: MutableRecipe = createSpecialRecipe(
        production = heat,
        category = SpecialNames.reactorRecipe,
        hint = "generating",
        products = listOf(MutableProduct(heat, 1f)),
        flags = RecipeFlags.SCALE_PRODUCTION_WITH_POWER
    )

    internal var character: MutableEntity? = null

    init {
        registerSpecial()
    }

    private fun createSpecialObject(
        isPower: Boolean,
        name: String,
        locName: String,
        locDescr: String,
        icon: String,
        signal: String
    ): MutableSpecial {
        return parent.getObject(name) {
            MutableSpecial(
                virtualSignal = signal,
                power = isPower,
                factorioType = "special",
                name = it,
                locName = locName,
                locDescr = locDescr,
                iconSpec = listOf(FactorioIconPart(icon)),
                fuelValue = if (isPower) 1f else 0f
            )
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
            if (parent.allObjects[from].sortingOrder != sortOrder) {
                break
            }
        }

        return from
    }

//    private void ExportBuiltData()
//    {
//        Database.rootAccessible = rootAccessible.ToArray()
//        Database.objectsByTypeName = allObjects.ToDictionary(x => x.typeDotName = x.type + "." + x.name)
//        foreach (var alias in formerAliases)
//        Database.objectsByTypeName.TryAdd(alias.Key, alias.Value)
//        Database.allSciencePacks = sciencePacks.ToArray()
//        Database.voidEnergy = voidEnergy
//        Database.electricity = electricity
//        Database.electricityGeneration = generatorProduction
//        Database.heat = heat
//        Database.character = character
//        var firstSpecial = 0
//        var firstItem = Skip(firstSpecial, FactorioObjectSortOrder.SpecialGoods)
//        var firstFluid = Skip(firstItem, FactorioObjectSortOrder.Items)
//        var firstRecipe = Skip(firstFluid, FactorioObjectSortOrder.Fluids)
//        var firstMechanics = Skip(firstRecipe, FactorioObjectSortOrder.Recipes)
//        var firstTechnology = Skip(firstMechanics, FactorioObjectSortOrder.Mechanics)
//        var firstEntity = Skip(firstTechnology, FactorioObjectSortOrder.Technologies)
//        var last = Skip(firstEntity, FactorioObjectSortOrder.Entities)
//        if (last != allObjects.Count)
//            throw new Exception("Something is not right")
//        Database.objects = new FactorioIdRange<FactorioObject>(0, last, allObjects)
//        Database.specials = new FactorioIdRange<Special>(firstSpecial, firstItem, allObjects)
//        Database.items = new FactorioIdRange<Item>(firstItem, firstFluid, allObjects)
//        Database.fluids = new FactorioIdRange<Fluid>(firstFluid, firstRecipe, allObjects)
//        Database.goods = new FactorioIdRange<Goods>(firstSpecial, firstRecipe, allObjects)
//        Database.recipes = new FactorioIdRange<Recipe>(firstRecipe, firstTechnology, allObjects)
//        Database.mechanics = new FactorioIdRange<Mechanics>(firstMechanics, firstTechnology, allObjects)
//        Database.recipesAndTechnologies = new FactorioIdRange<RecipeOrTechnology>(firstRecipe, firstEntity, allObjects)
//        Database.technologies = new FactorioIdRange<Technology>(firstTechnology, firstEntity, allObjects)
//        Database.entities = new FactorioIdRange<Entity>(firstEntity, last, allObjects)
//        Database.fluidVariants = fluidVariants
//
//        Database.allModules = allModules
//        Database.allBeacons = Database.entities.all.OfType<EntityBeacon>().ToArray()
//        Database.allCrafters = Database.entities.all.OfType<EntityCrafter>().ToArray()
//        Database.allBelts = Database.entities.all.OfType<EntityBelt>().ToArray()
//        Database.allInserters = Database.entities.all.OfType<EntityInserter>().ToArray()
//        Database.allAccumulators = Database.entities.all.OfType<EntityAccumulator>().ToArray()
//        Database.allContainers = Database.entities.all.OfType<EntityContainer>().ToArray()
//    }

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

//    private fun calculateMaps() {
//        val itemUsages = DataBucket<Goods, Recipe>()
//        val itemProduction = DataBucket<Goods, Recipe>()
//        val miscSources = DataBucket<Goods, FactorioObject>()
//        val entityPlacers = DataBucket<Entity, Item>()
//        val recipeUnlockers = DataBucket<Recipe, Technology>()
//        // Because actual recipe availibility may be different than just "all recipes from that category" because of item slot limit and fluid usage restriction, calculate it here
//        val actualRecipeCrafters = DataBucket<RecipeOrTechnology, EntityCrafter>()
//        val usageAsFuel = DataBucket<Goods, Entity>()
//        val allRecipes = arrayListOf<Recipe>()
//        val allMechanics = arrayListOf<Mechanics>()
//
//        // step 1 - collect maps
//
//        for (o in allObjects) {
//            when (o) {
//                is Technology -> {
//                    for (recipe in o.unlockRecipes) {
//                        recipeUnlockers[recipe] = o
//                    }
//                }
//                is Recipe -> {
//                    allRecipes.add(o)
//
//                    for (product in o.products) {
//                        if (product.amount > 0) {
//                            itemProduction[product.goods] = o
//                        }
//                    }
//
//                    for (ingredient in o.ingredients) {
//                        if (ingredient.variants == null) {
//                            itemUsages[ingredient.goods] = o
//                        } else {
//                            ingredient.goods = ingredient.variants[0]
//                            for (variant in ingredient.variants) {
//                                itemUsages[variant] = o
//                            }
//                        }
//                    }
//
//                    if (o is Mechanics) {
//                        allMechanics.add(o)
//                    }
//                }
//                is Item -> {
//                    val placeResultStr = placeResults[o]
//                    if (placeResultStr != null) {
//                        o.placeResult = getObject(placeResultStr) as Entity
//                        entityPlacers.add(o.placeResult, o)
//                    }
//
//                    if (o.fuelResult != null) {
//                        miscSources.add(o.fuelResult, o)
//                    }
//                }
//                is Entity -> {
//                    for (product in entity.loot) {
//                        miscSources.add(product.goods, o)
//                    }
//
//                    if (o is EntityCrafter) {
//                        o.recipes = recipeCrafters.getRaw(o).SelectMany(x => recipeCategories . GetRaw (x).Where(y => y . CanFit (crafter.itemInputs, crafter.fluidInputs, crafter.inputs))).ToArray()
//                        for (recipe in crafter.recipes) {
//                            actualRecipeCrafters[recipe] = o
//                        }
//                    }
//
//                    if (o.energy != null && o.energy != voidEntityEnergy) {
//                        val fuelList = fuelUsers.GetRaw(entity).SelectMany(fuels.GetRaw)
//                        if (entity.energy.type == EntityEnergyType.FluidHeat) {
//                            fuelList =
//                                fuelList.Where(x => x is Fluid f && entity.energy.acceptedTemperature.Contains(f.temperature) && f.temperature > entity.energy.workingTemperature.min)
//                        }
//
//                        val fuelListArr = fuelList.ToArray()
//                        o.energy.fuels = fuelListArr
//
//                        for (fuel in fuelListArr) {
//                            usageAsFuel[fuel] = o
//                        }
//                    }
//                }
//            }
//        }
//
//        voidEntityEnergy.fuels = new Goods[] {voidEnergy}
//
//        actualRecipeCrafters.SealAndDeduplicate()
//        usageAsFuel.SealAndDeduplicate()
//        recipeUnlockers.SealAndDeduplicate()
//        entityPlacers.SealAndDeduplicate()
//
//        // step 2 - fill maps
//
//        for (o in allObjects) {
//            when (o) {
//                case RecipeOrTechnology recipeOrTechnology:
//                    if (recipeOrTechnology is Recipe recipe)
//            {
//                recipe.FallbackLocalization(recipe.mainProduct, "A recipe to create")
//                recipe.technologyUnlock = recipeUnlockers.GetArray(recipe)
//            }
//                recipeOrTechnology.crafters = actualRecipeCrafters.GetArray(recipeOrTechnology)
//                    break
//                    case Goods goods:
//                goods.usages = itemUsages.GetArray(goods)
//                goods.production = itemProduction.GetArray(goods)
//                        goods.miscSources = miscSources.GetArray(goods)
//                    if (o is Item item)
//            {
//                if (item.placeResult != null)
//                    item.FallbackLocalization(item.placeResult, "An item to build")
//            } else if (o is Fluid fluid && fluid.variants != null)
//            {
//                var temperatureDescr = "Temperature: " + fluid.temperature + "°"
//                if (fluid.locDescr == null)
//                    fluid.locDescr = temperatureDescr
//                else fluid.locDescr = temperatureDescr + "\n" + fluid.locDescr
//            }
//
//                goods.fuelFor = usageAsFuel.GetArray(goods)
//                    break
//                    case Entity entity:
//                entity.itemsToPlace = entityPlacers.GetArray(entity)
//                break
//            }
//        }
//
//        foreach (var mechanic in allMechanics)
//        {
//            mechanic.locName = mechanic.source.locName + " " + mechanic.locName
//            mechanic.locDescr = mechanic.source.locDescr
//            mechanic.iconSpec = mechanic.source.iconSpec
//        }
//
//        // step 3 - detect barreling/unbarreling and voiding recipes
//        foreach (var recipe in allRecipes)
//        {
//            if (recipe.specialType != FactorioObjectSpecialType.Normal)
//                continue
//            if (recipe.products.Length == 0)
//            {
//                recipe.specialType = FactorioObjectSpecialType.Voiding
//                continue
//            }
//            if (recipe.products.Length != 1 || recipe.ingredients.Length == 0)
//                continue
//            if (recipe.products[0].goods is Item barrel)
//            {
//                foreach (var usage in barrel.usages)
//                {
//                    if (IsBarrelingRecipe(recipe, usage))
//                    {
//                        recipe.specialType = FactorioObjectSpecialType.Barreling
//                        usage.specialType = FactorioObjectSpecialType.Unbarreling
//                        barrel.specialType = FactorioObjectSpecialType.FilledBarrel
//                    }
//                }
//            }
//        }
//
//        foreach (var any in allObjects)
//        {
//            if (any.locName == null)
//                any.locName = any.name
//        }
//
//        foreach (var (_, list) in fluidVariants)
//        {
//            foreach (var fluid in list)
//            {
//                fluid.locName += " " + fluid.temperature + "°"
//            }
//        }
//    }

    internal fun createSpecialRecipe(
        production: MutableFactorioObject,
        category: String,
        hint: String,
        products: List<MutableProduct> = emptyList(),
        ingredients: List<MutableIngredient> = emptyList(),
        flags: RecipeFlags? = null
    ): MutableRecipe {
        val fullName = "$category${(if (category.endsWith(".")) "" else ".")}${production.name}"

        val recipeRaw = parent.registeredObjects[typeWithName<MutableMechanics>(fullName)] as? MutableMechanics
        if (recipeRaw != null) {
            return recipeRaw
        }

        val recipe = parent.getObject(fullName) {
            MutableMechanics(
                source = production,
                factorioType = SpecialNames.fakeRecipe,
                name = fullName,
                locName = hint,
                ingredients = ingredients,
                products = products,
                time = 1f,
                enabled = true,
                hidden = true,
                flags = flags
            )
        }

        parent.recipeCategories.add(category, recipe)

        return recipe
    }

    private fun FactorioObject.tryCastToTypeByString(typeName: String): FactorioObject? {
        return when (typeName) {
            "item" -> this as? Item
            "fluid" -> this as? Fluid
            "technology" -> this as? Technology
            "recipe" -> this as? Recipe
            "entity" -> this as? Entity
            else -> null
        }
    }
}
