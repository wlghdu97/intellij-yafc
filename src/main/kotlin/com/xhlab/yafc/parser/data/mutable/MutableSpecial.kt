package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.data.entity.Entity
import javax.swing.Icon

internal data class MutableSpecial(
    override val name: String,
    override var factorioType: String?,
    override var originalName: String? = null,
    override var typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override var icon: Icon? = null,
    override var id: FactorioId? = null,
    val virtualSignal: String,
    internal val power: Boolean,
    override val fuelValue: Float = 0f,
    override val production: List<Recipe> = emptyList(),
    override val usages: List<Recipe> = emptyList(),
    override val miscSources: List<FactorioObject> = emptyList(),
    override val fuelFor: List<Entity> = emptyList()
) : MutableGoods() {
    override val isPower: Boolean = power
    override val type: String = if (isPower) "Power" else "Special"
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.SPECIAL_GOODS
    override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.PER_SECOND
}
