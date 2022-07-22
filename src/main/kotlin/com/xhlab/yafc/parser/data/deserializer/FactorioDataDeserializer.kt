package com.xhlab.yafc.parser.data.deserializer

import com.xhlab.yafc.model.Project
import com.xhlab.yafc.model.Version
import com.xhlab.yafc.model.data.Fluid
import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityCrafter
import com.xhlab.yafc.parser.data.deserializer.FactorioDataDeserializer.TypeWithName.Companion.typeWithName
import com.xhlab.yafc.parser.data.mutable.*
import org.luaj.vm2.LuaTable
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class FactorioDataDeserializer constructor(
    projectPath: String,
    data: LuaTable,
    prototypes: LuaTable,
    renderIcons: Boolean,
    expensiveRecipes: Boolean,
    factorioVersion: Version
) {
    internal val allObjects = arrayListOf<MutableFactorioObject>()
    internal val rootAccessible = arrayListOf<MutableFactorioObject>()
    internal val registeredObjects = hashMapOf<TypeWithName, MutableFactorioObject>()

    internal val fuels = DataBucket<String, MutableGoods>() // DataBucket
    internal val fuelUsers = DataBucket<Entity, String>() // DataBucket
    internal val recipeCategories = DataBucket<String, MutableRecipeOrTechnology>() // DataBucket
    internal val recipeCrafters = DataBucket<EntityCrafter, String>() // DataBucket
    internal val recipeModules = DataBucket<MutableRecipe, MutableItem>() // DataBucket
    internal val placeResults = hashMapOf<MutableItem, String>()
    internal val universalModules = arrayListOf<MutableItem>()
    internal val allModules = arrayListOf<MutableItem>()
    internal val sciencePacks = hashSetOf<MutableItem>()
    internal val fluidVariants = hashMapOf<String, List<Fluid>>()
    internal val formerAliases = hashMapOf<String, MutableFactorioObject>()
    internal val rocketInventorySizes = hashMapOf<String, Int>()

    internal val common = CommonDeserializer(this, projectPath, data, prototypes, renderIcons)
    internal val context = ContextDeserializer(this, expensiveRecipes, factorioVersion)
    internal val recipeAndTechnology = RecipeAndTechnologyDeserializer(this)

    fun loadData(): Project {
        return common.loadData()
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
        val key = typeWithName<T>(name)
        val existing = registeredObjects[key] as? T
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
