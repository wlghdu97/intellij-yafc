package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityCrafter
import javax.swing.Icon

internal data class Mechanics(
    val source: FactorioObject,
    override val factorioType: String,
    override val name: String,
    override val originalName: String? = null,
    override val typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override val icon: Icon? = null,
    override val id: FactorioId? = null,
    override val specialType: FactorioObjectSpecialType? = null,
    override val recipe: RecipeOrTechnology? = null,
    override val technologyUnlock: List<Technology> = emptyList(),
    override val crafters: List<EntityCrafter> = emptyList(),
    override val ingredients: List<Ingredient> = emptyList(),
    override val products: List<Product> = emptyList(),
    override val sourceEntity: Entity? = null,
    override val mainProduct: Goods? = null,
    override val time: Float,
    override val enabled: Boolean,
    override val hidden: Boolean,
    override val flags: RecipeFlags?
) : MutableRecipe() {
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.MECHANICS
    override val type: String = "Mechanics"
}

