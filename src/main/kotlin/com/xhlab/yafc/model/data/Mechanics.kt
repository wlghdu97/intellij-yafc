package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.data.entity.Entity
import com.xhlab.yafc.model.data.entity.EntityCrafter
import javax.swing.Icon

data class Mechanics(
    val source: FactorioObject,
    override val factorioType: String,
    override val name: String,
    override val originalName: String,
    override val typeDotName: String,
    override val locName: String,
    override val locDescr: String,
    override val iconSpec: List<FactorioIconPart>,
    override val icon: Icon,
    override val id: FactorioId,
    override val specialType: FactorioObjectSpecialType,
    override val technologyUnlock: List<Technology>,
    override val crafters: List<EntityCrafter>,
    override val ingredients: List<Ingredient>,
    override val products: List<Product>,
    override val sourceEntity: Entity?,
    override val mainProduct: Goods?,
    override val time: Float,
    override val enabled: Boolean,
    override val hidden: Boolean,
    override val flags: RecipeFlags?
) : Recipe() {
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.MECHANICS
    override val type: String = "Mechanics"
}
