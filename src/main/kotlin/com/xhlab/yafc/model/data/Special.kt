package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.data.entity.Entity
import javax.swing.Icon

data class Special(
    val virtualSignal: String,
    internal val power: Boolean,
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
    override val fuelValue: Float = 0f,
    override val production: List<Recipe> = emptyList(),
    override val usages: List<Recipe> = emptyList(),
    override val miscSources: List<FactorioObject> = emptyList(),
    override val fuelFor: List<Entity> = emptyList()
) : Goods() {
    override val isPower: Boolean = power
    override val type: String = if (isPower) "Power" else "Special"
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.SPECIAL_GOODS
    override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.PER_SECOND
}
