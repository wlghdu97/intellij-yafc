package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.data.entity.Entity
import javax.swing.Icon

data class Item(
    val fuelResult: Item?,
    val stackSize: Int,
    val placeResult: Entity?,
    val module: ModuleSpecification?,
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
    override val fuelFor: List<Entity>,
) : Goods() {
    override val isPower: Boolean = false
    override val type: String = "Item"
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.ITEMS
    override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.ITEM_PER_SECOND
}
