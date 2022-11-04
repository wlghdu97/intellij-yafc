package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.IDependencyCollector
import javax.swing.Icon

sealed interface FactorioObject : IFactorioObjectWrapper, Comparable<FactorioObject> {
    val factorioType: String
    val name: String
    val originalName: String // name without temperature
    val locName: String
    val locDescr: String
    val iconSpec: List<FactorioIconPart>
    val icon: Icon?
    val id: FactorioId

    val sortingOrder: FactorioObjectSortOrder
        get() = FactorioObjectSortOrder.NONE
    val specialType: FactorioObjectSpecialType
        get() = FactorioObjectSpecialType.NORMAL
    val type: String

    // changed typeDotName as calculated property, it's deterministic, can be used to distinguish same named objects with this
    val typeDotName: String
        get() = "$type.$name"

    override val target: FactorioObject
        get() = this
    override val amount: Float
        get() = 1f
    override val text: String
        get() = locName

    fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>)

    override fun compareTo(other: FactorioObject): Int {
        return DataUtils.defaultOrdering.compare(this, other)
    }
}
