package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.data.entity.Entity
import javax.swing.Icon

data class Item(
    val fuelResult: Item? = null,
    var stackSize: Int = 0,
    val placeResult: Entity? = null,
    val module: ModuleSpecification? = null,
    override val factorioType: String = "",
    override val name: String = "",
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
    override val fuelFor: List<Entity> = emptyList(),
) : Goods() {

    override val isPower: Boolean = false
    override val type: String = "Item"
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.ITEMS
    override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.ITEM_PER_SECOND
}
