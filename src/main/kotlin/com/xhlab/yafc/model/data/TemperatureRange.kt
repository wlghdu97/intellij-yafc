package com.xhlab.yafc.model.data

data class TemperatureRange(val min: Int, val max: Int) {

    constructor(single: Int) : this(single, single)

    override fun toString(): String {
        if (min == max) {
            return "$min°"
        }
        return "$min°-$max°"
    }

    fun isAny() = (min == Any.min && max == Any.max)

    fun isSingle() = (min == max)

    fun contains(value: Int) = (value in min..max)

    companion object {
        val Any = TemperatureRange(Int.MIN_VALUE, Int.MAX_VALUE)
    }
}
