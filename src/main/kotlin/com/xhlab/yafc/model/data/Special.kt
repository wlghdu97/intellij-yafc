package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.data.entity.Entity
import javax.swing.Icon

data class Special(
    val virtualSignal: String,
    internal val power: Boolean,
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
    override val fuelValue: Float,
    override val production: List<Recipe>,
    override val usages: List<Recipe>,
    override val miscSources: List<FactorioObject>,
    override val fuelFor: List<Entity>
) : Goods() {
    override val isPower: Boolean = power
    override val type: String = if (isPower) "Power" else "Special"
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.SPECIAL_GOODS
    override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.PER_SECOND
}
