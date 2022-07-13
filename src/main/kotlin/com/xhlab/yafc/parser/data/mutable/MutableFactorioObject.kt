package com.xhlab.yafc.parser.data.mutable

import com.xhlab.yafc.model.analysis.IDependencyCollector
import com.xhlab.yafc.model.data.*
import javax.swing.Icon

internal open class MutableFactorioObject(
    override val name: String,
    override var factorioType: String? = null,
    override var originalName: String? = null,
    override var typeDotName: String? = null,
    override var locName: String? = null,
    override var locDescr: String? = null,
    override var iconSpec: List<FactorioIconPart>? = null,
    override var icon: Icon? = null,
    override var id: FactorioId? = null,
    override val sortingOrder: FactorioObjectSortOrder = FactorioObjectSortOrder.NONE,
    override val specialType: FactorioObjectSpecialType? = null,
    override val type: String? = null
) : FactorioObject() {

    fun fallbackLocalization(other: FactorioObject?, description: String) {
        if (locName == null) {
            if (other == null) {
                locName = name
            } else {
                locName = other.locName
                locDescr = "$description $locName"
            }
        }

        if (iconSpec == null && other?.iconSpec != null) {
            iconSpec = other.iconSpec
        }
    }

    override fun getDependencies(collector: IDependencyCollector, temp: MutableList<FactorioObject>) {
        throw UnsupportedOperationException("getting dependencies on mutable is not supported")
    }
}
