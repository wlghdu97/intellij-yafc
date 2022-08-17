package com.xhlab.yafc.model.data

import javax.swing.Icon

internal sealed interface MutableFactorioObject : FactorioObject {
    override var factorioType: String
    override var originalName: String
    override var typeDotName: String
    override var locName: String
    override var locDescr: String
    override var iconSpec: List<FactorioIconPart>
    override var icon: Icon?
    override var id: FactorioId
    override var specialType: FactorioObjectSpecialType

    fun fallbackLocalization(other: MutableFactorioObject?, description: String) {
        if (locName.isEmpty()) {
            if (other == null) {
                locName = name
            } else {
                locName = other.locName
                locDescr = "$description $locName"
            }
        }

        if (iconSpec.isEmpty() && other?.iconSpec != null && other.iconSpec.isNotEmpty()) {
            iconSpec = other.iconSpec
        }
    }
}
