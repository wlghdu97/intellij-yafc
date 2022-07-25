package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.data.entity.Entity
import javax.swing.Icon

internal data class MutableItem(
    override val name: String,
    override var factorioType: String? = null,
    override var originalName: String? = null,
    override var typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override var icon: Icon? = null,
    override var id: FactorioId? = null,
    var fuelResult: MutableItem? = null,
    var stackSize: Int = 0,
    var placeResult: Entity? = null,
    var module: MutableModuleSpecification? = null,
    override var fuelValue: Float = 0f,
    override var production: List<Recipe> = emptyList(),
    override var usages: List<Recipe> = emptyList(),
    override var miscSources: List<FactorioObject> = emptyList(),
    override var fuelFor: List<Entity> = emptyList(),
) : MutableGoods() {
    override val isPower: Boolean = false
    override val type: String = "Item"
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.ITEMS
    override val flowUnitOfMeasure: UnitOfMeasure = UnitOfMeasure.ITEM_PER_SECOND

//    override fun hasSpentFuel(spent: MutableItem): Pair<Boolean, MutableItem> {
//        spent = fuelResult
//        return spent != null
//    }

    companion object {
        fun MutableFactorioObject.toItem() = MutableItem(
            name = name,
            factorioType = factorioType,
            originalName = originalName,
            typeDotName = typeDotName,
            locName = locName,
            locDescr = locDescr,
            iconSpec = iconSpec,
            icon = icon,
            id = id
        )
    }
}
