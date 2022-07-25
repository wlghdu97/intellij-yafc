package com.xhlab.yafc.parser.data.deserializer

import com.xhlab.yafc.model.data.RecipeFlags
import com.xhlab.yafc.model.data.TemperatureRange
import com.xhlab.yafc.parser.data.SpecialNames
import com.xhlab.yafc.parser.data.mutable.*
import org.luaj.vm2.LuaTable

class RecipeAndTechnologyDeserializer constructor(
    private val parent: FactorioDataDeserializer,
    private val expensiveRecipes: Boolean
) {
    private inline fun <reified T> deserializeWithDifficulty(
        table: LuaTable,
        prototypeType: String,
        construct: (name: String) -> T,
        loader: (T, LuaTable, Boolean) -> Unit
    ): T where T : MutableFactorioObject {
        val obj = parent.common.deserializeCommon(table, prototypeType, construct)

        val current = if (expensiveRecipes) table["expensive"] else table["normal"]
        val fallback = if (expensiveRecipes) table["normal"] else table["expensive"]
        when {
            (current.istable()) -> {
                loader(obj, current.checktable(), false)
            }

            (fallback.istable()) -> {
                val forceDisable = current.isboolean() && !current.toboolean()
                loader(obj, fallback.checktable(), forceDisable)
            }

            else -> {
                loader(obj, table, false)
            }
        }

        return obj
    }

    internal val recipeDeserializer = object : CommonDeserializer.Deserializer {
        override fun deserialize(table: LuaTable) {
            val recipe =
                deserializeWithDifficulty(table, "recipe", ::MutableRecipeImpl) { recipe, luaTable, forceDisable ->
                    loadRecipeData(recipe, luaTable, forceDisable)
                }

            val recipeCategory = table["category"].optjstring("crafting")
            parent.recipeCategories.add(recipeCategory, recipe)
            recipe.modules = parent.recipeModules.getList(recipe)
            recipe.flags = RecipeFlags.LIMITED_BY_TICK_RATE or recipe.flags
        }
    }

    private fun deserializeFlags(table: LuaTable, recipe: MutableRecipeOrTechnology, forceDisable: Boolean) {
        recipe.hidden = table["hidden"].optboolean(true)
        recipe.enabled = if (forceDisable) {
            false
        } else {
            table["enabled"].optboolean(true)
        }
    }

    internal val technologyDeserializer = object : CommonDeserializer.Deserializer {
        override fun deserialize(table: LuaTable) {
            val technology =
                deserializeWithDifficulty(table, "technology", ::MutableTechnology) { tech, luaTable, forceDisable ->
                    loadTechnologyData(tech, luaTable, forceDisable)
                }
            parent.recipeCategories.add(SpecialNames.labs, technology)
            technology.products = emptyList()
        }
    }

//    private void UpdateRecipeCatalysts()
//    {
//        foreach (var recipe in allObjects.OfType<Recipe>())
//        {
//            foreach (var product in recipe.products)
//            {
//                if (product.productivityAmount == product.amount)
//                {
//                    var catalyst = recipe.GetConsumption(product.goods);
//                    if (catalyst > 0f)
//                        product.SetCatalyst(catalyst);
//                }
//            }
//        }
//    }
//
//    private void UpdateRecipeIngredientFluids()
//    {
//        foreach (var recipe in allObjects.OfType<Recipe>())
//        {
//            foreach (var ingredient in recipe.ingredients)
//            {
//                if (ingredient.goods is Fluid fluid && fluid.variants != null)
//                {
//                    int min = -1, max = fluid.variants.Count-1;
//                    for (var i = 0; i < fluid.variants.Count; i++)
//                    {
//                        var variant = fluid.variants[i];
//                        if (variant.temperature < ingredient.temperature.min)
//                            continue;
//                        if (min == -1)
//                            min = i;
//                        if (variant.temperature > ingredient.temperature.max)
//                        {
//                            max = i - 1;
//                            break;
//                        }
//                    }
//
//                    if (min >= 0 && max >= 0)
//                    {
//                        ingredient.goods = fluid.variants[min];
//                        if (max > min)
//                        {
//                            var fluidVariants = new Fluid[max - min + 1];
//                            ingredient.variants = fluidVariants;
//                            fluid.variants.CopyTo(min, fluidVariants, 0, max-min+1);
//                        }
//                    }
//                }
//            }
//        }
//    }

    private fun loadTechnologyData(technology: MutableTechnology, table: LuaTable, forceDisable: Boolean) {
        val unit = table["unit"].checktable()
        technology.ingredients = loadIngredientList(unit)
        deserializeFlags(table, technology, forceDisable)

        technology.time = unit["time"].optdouble(1.0).toFloat()
        technology.count = unit["count"].optdouble(1000.0).toFloat()

        val prerequisites = table["prerequisites"].opttable(null)
        if (prerequisites != null) {
            technology.prerequisites = prerequisites.keys().asSequence()
                .mapNotNull { prerequisites[it].optjstring(null) }
                .map { parent.getObject(it, ::MutableTechnology) }
                .toList()
        }

        val modifiers = table["effects"].opttable(null)
        if (modifiers != null) {
            technology.unlockRecipes = modifiers.keys().asSequence()
                .mapNotNull { modifiers[it].opttable(null) }
                .mapNotNull {
                    val type = it["type"].optjstring("")
                    if (type == "unlock-recipe") {
                        parent.getRef(it, "recipe", ::MutableRecipeImpl).second
                    } else {
                        null
                    }
                }.toList()
        }
    }

    internal fun loadProduct(table: LuaTable) = loadProductWithMultiplier(table, 1)

    internal fun loadProductWithMultiplier(table: LuaTable, multiplier: Int): MutableProduct? {
        var (goods, amount, haveExtraData) = parent.common.loadItemData(table, true)
        if (goods == null) {
            return null
        }

        amount *= multiplier

        var min = amount
        var max = amount
        if (haveExtraData && amount == 0f) {
            min = table["amount_min"].todouble().toFloat() * multiplier
            max = table["amount_max"].todouble().toFloat() * multiplier
        }

        val probability = table["probability"].optdouble(1.0).toFloat()
        val product = MutableProduct(goods, min, max, probability)
        val catalyst = table["catalyst_amount"].optdouble(0.0).toFloat()
        if (catalyst > 0f) {
            product.setCatalyst(catalyst)
        }

        return product
    }

    private fun loadProductList(table: LuaTable): List<MutableProduct> {
        val resultList = table["results"]
        if (resultList.istable()) {
            return resultList.checktable().keys().asSequence()
                .mapNotNull { resultList[it].opttable(null) }
                .mapNotNull { loadProduct(it) }
                .filter { it.amount != 0f }
                .toList()
        }

        val name = table["result"].optjstring(null)
        if (name != null) {
            val goods = parent.getObject(name, ::MutableItem)
            val amount = table["result_count"].optdouble(table["count"].optint(1).toDouble()).toFloat()
            val singleProduct = MutableProduct(goods, amount)
            return listOf(singleProduct)
        }

        return emptyList()
    }

    private fun loadIngredientList(table: LuaTable): List<MutableIngredient> {
        val ingredients = table["ingredients"]
        return if (ingredients.istable()) {
            ingredients.checktable().keys().mapNotNull {
                val element = ingredients[it].checktable()
                val (goods, amount, haveExtraData) = parent.common.loadItemData(element, false)
                if (goods == null) {
                    return@mapNotNull null
                }

                val ingredient = MutableIngredient(goods, amount)
                if (haveExtraData && goods is MutableFluid) {
                    val temperature = element["temperature"]
                    ingredient.temperature = if (temperature.isint()) {
                        TemperatureRange(temperature.toint())
                    } else {
                        val min = element["minimum_temperature"].optint(goods.temperatureRange.min)
                        val max = element["maximum_temperature"].optint(goods.temperatureRange.max)
                        TemperatureRange(min, max)
                    }
                }

                ingredient
            }
        } else {
            emptyList()
        }
    }

    private fun loadRecipeData(recipe: MutableRecipeImpl, table: LuaTable, forceDisable: Boolean) {
        recipe.ingredients = loadIngredientList(table)
        recipe.products = loadProductList(table)

        recipe.time = table["energy_required"].optdouble(0.5).toFloat()

        val mainProductName = table["main_product"].optjstring(null)
        if (mainProductName != null && mainProductName != "") {
            recipe.mainProduct = recipe.products.firstOrNull { it.goods.name == mainProductName }?.goods
        } else if (recipe.products.size == 1) {
            recipe.mainProduct = recipe.products[0].goods
        }

        deserializeFlags(table, recipe, forceDisable)
    }
}
