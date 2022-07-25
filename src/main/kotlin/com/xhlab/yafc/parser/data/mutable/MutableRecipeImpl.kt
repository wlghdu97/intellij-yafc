package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityCrafter
import javax.swing.Icon

internal data class MutableRecipeImpl(
    override val name: String,
    override var factorioType: String? = null,
    override var originalName: String? = null,
    override var typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override var icon: Icon? = null,
    override var id: FactorioId? = null,
    override var fuelResult: MutableItem? = null,
    override var recipe: RecipeOrTechnology? = null,
    override var technologyUnlock: List<Technology> = emptyList(),
    override var crafters: List<EntityCrafter> = emptyList(),
    override var ingredients: List<MutableIngredient> = emptyList(),
    override var products: List<MutableProduct> = emptyList(),
    override var sourceEntity: Entity? = null,
    override var mainProduct: MutableGoods? = null,
    override var time: Float = 0f,
    override var enabled: Boolean = false,
    override var hidden: Boolean = false,
    override var flags: RecipeFlags? = null
) : MutableRecipe()
