package com.xhlab.yafc.model.data

import java.util.*
import javax.swing.Icon

internal data class MutableTechnology(
    override val name: String
) : Technology(), MutableRecipeOrTechnology { // Technology is very similar to recipe
    override var factorioType: String = ""
    override var originalName: String = ""
    override var locName: String = ""
    override var locDescr: String = ""
    override var iconSpec: List<FactorioIconPart> = emptyList()
    override var icon: Icon? = null
    override var id: FactorioId = FactorioId(-1)
    override var specialType: FactorioObjectSpecialType = FactorioObjectSpecialType.NORMAL
    override var crafters: List<MutableEntityCrafter> = emptyList()
    override var ingredients: List<MutableIngredient> = emptyList()
    override var products: List<MutableProduct> = emptyList()
    override var modules: List<MutableItem> = emptyList()
    override var sourceEntity: MutableEntity? = null
    override var mainProduct: MutableGoods? = null
    override var time: Float = 0f
    override var enabled: Boolean = false
    override var hidden: Boolean = false
    override var flags: RecipeFlags = EnumSet.noneOf(RecipeFlag::class.java)
    override var count: Float = 0f // TODO support formula count
    override var unlockRecipes: List<MutableRecipe> = emptyList()
    override var prerequisites: List<MutableTechnology> = emptyList()
}
