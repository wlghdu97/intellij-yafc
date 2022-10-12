package com.xhlab.yafc.parser.data.deserializer

import com.xhlab.yafc.model.Version
import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.parser.ProgressTextIndicator
import com.xhlab.yafc.parser.data.deserializer.FactorioDataDeserializer.TypeWithName.Companion.typeWithName
import org.luaj.vm2.LuaTable
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class FactorioDataDeserializer constructor(
    data: LuaTable,
    prototypes: LuaTable,
    expensiveRecipes: Boolean,
    factorioVersion: Version
) {
    internal val allObjects = arrayListOf<MutableFactorioObject>()
    internal val rootAccessible = arrayListOf<MutableFactorioObject>()
    internal val registeredObjects = hashMapOf<TypeWithName, MutableFactorioObject>()

    internal val fuels = DataBucket<String, MutableGoods>() // DataBucket
    internal val fuelUsers = DataBucket<MutableEntity, String>() // DataBucket
    internal val recipeCategories = DataBucket<String, MutableRecipeOrTechnology>() // DataBucket
    internal val recipeCrafters = DataBucket<MutableEntityCrafter, String>() // DataBucket
    internal val recipeModules = DataBucket<MutableRecipe, MutableItem>() // DataBucket
    internal val placeResults = hashMapOf<MutableItem, String>()
    internal val universalModules = arrayListOf<MutableItem>()
    internal val allModules = arrayListOf<MutableItem>()
    internal val sciencePacks = hashSetOf<MutableItem>()
    internal val fluidVariants = hashMapOf<String, List<MutableFluid>>()
    internal val formerAliases = hashMapOf<String, MutableFactorioObject>()
    internal val rocketInventorySizes = hashMapOf<String, Int>()

    internal val common = CommonDeserializer(this, data, prototypes)
    internal val context = ContextDeserializer(this)
    internal val entity = EntityDeserializer(this, factorioVersion)
    internal val recipeAndTechnology = RecipeAndTechnologyDeserializer(this, expensiveRecipes)

    fun loadData(progress: ProgressTextIndicator): YAFCDatabase {
        return common.loadData(progress)
    }

    internal inline fun <reified T> getRef(
        table: LuaTable,
        key: String,
        construct: (name: String) -> T
    ): Pair<Boolean, T?> where T : MutableFactorioObject {
        val name = table[key]
        if (!name.isstring()) {
            return false to null
        }

        return true to getObject(name.tojstring(), construct)
    }

    internal inline fun <reified T> getObject(
        name: String,
        construct: (name: String) -> T
    ): T where T : MutableFactorioObject {
        return getObjectWithNominal<T, T>(name, construct)
    }

    internal inline fun <reified Nominal, reified Actual> getObjectWithNominal(
        name: String,
        construct: (name: String) -> Actual
    ): Actual where Nominal : MutableFactorioObject, Actual : MutableFactorioObject {
        val key = typeWithName<Nominal>(name)
        val existing = registeredObjects[key] as? Actual
        if (existing != null) {
            return existing
        }

        // sometimes target object is not yet created.
        val newItem = construct.invoke(name)
        allObjects.add(newItem)
        registeredObjects[key] = newItem

        return newItem
    }

    data class TypeWithName(val type: KType, val name: String) {
        companion object {
            inline fun <reified T> typeWithName(name: String): TypeWithName where T : MutableFactorioObject {
                return TypeWithName(typeOf<T>(), name)
            }
        }
    }
}
