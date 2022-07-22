package com.xhlab.yafc.model.data

import com.xhlab.yafc.model.analysis.IDependencyCollector
import javax.swing.Icon

abstract class FactorioObject : IFactorioObjectWrapper, Comparable<FactorioObject> {
    abstract val factorioType: String?
    abstract val name: String
    abstract val originalName: String? // name without temperature
    abstract val typeDotName: String?
    abstract val locName: String?
    abstract val locDescr: String?
    abstract val iconSpec: List<FactorioIconPart>?
    abstract val icon: Icon?
    abstract val id: FactorioId?

    internal abstract val sortingOrder: FactorioObjectSortOrder
    abstract val specialType: FactorioObjectSpecialType?
    abstract val type: String?

    override val target: FactorioObject
        get() = this
    override val amount: Float
        get() = 1f
    override val text: String
        get() = locName ?: ""

    abstract fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>)

    override fun toString(): String {
        return name
    }

    override fun compareTo(other: FactorioObject): Int {
        return DataUtils.defaultOrdering.compare(this, other)
    }
}
