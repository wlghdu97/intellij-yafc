package com.xhlab.yafc.model.data

data class FactorioId(val id: Int) : Comparable<FactorioId> {

    override fun compareTo(other: FactorioId): Int {
        return id.compareTo(other.id)
    }
}
