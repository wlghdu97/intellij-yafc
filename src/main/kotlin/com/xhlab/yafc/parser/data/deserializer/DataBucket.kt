package com.xhlab.yafc.parser.data.deserializer

import com.xhlab.yafc.parser.data.deserializer.DataBucket.DataBucketList.Companion.toDataBucketList

class DataBucket<TKey, TValue> {

    private val storage = hashMapOf<TKey, List<TValue>>()
    private val def = arrayListOf<TValue>()
    private var sealed = false

    // Replaces lists in storage with arrays. List with same contents get replaced with the same arrays
    fun sealAndDeduplicate(addExtra: List<TValue> = emptyList(), sort: Comparator<TValue>? = null) {
        if (addExtra.isNotEmpty()) {
            def.clear()
            def.addAll(addExtra)
        }

        val mapDict = hashMapOf<DataBucketList<TValue>, List<TValue>>()
        for ((key, list) in storage.entries) {
            val dataBucketList = list.toDataBucketList()
            val prev = mapDict[dataBucketList]
            if (prev != null) {
                storage[key] = prev
            } else {
                val mergedList = if (addExtra.isEmpty()) list else list + addExtra
                val result = if (sort != null && mergedList.size > 1) {
                    mergedList.sortedWith(sort)
                } else {
                    mergedList
                }

                mapDict[dataBucketList] = result
                storage[key] = result
            }
        }

        sealed = true
    }

    fun add(key: TKey, value: TValue, checkUnique: Boolean = false) {
        if (sealed) {
            throw IllegalStateException("Data bucket is sealed")
        }
        if (key == null) {
            return
        }

        val list = storage[key]
        if (list == null) {
            storage[key] = listOf(value)
        } else if (!checkUnique || !list.contains(value)) {
            storage[key] = list + value
        }
    }

    fun getList(key: TKey): List<TValue> {
        return storage[key] ?: def
    }

    fun getRaw(key: TKey): List<TValue> {
        val list = storage[key]
        return if (list == null) {
            storage[key] = def
            def
        } else {
            list
        }
    }

    private class DataBucketList<T> constructor(collection: Collection<T>) : List<T> by ArrayList(collection) {

        override fun equals(other: Any?): Boolean {
            if (other !is DataBucketList<*>) {
                return false
            }
            if (size != other.size) {
                return false
            }

            for (i in 0 until size) {
                if (this[i] != other[i]) {
                    return false
                }
            }

            return true
        }

        override fun hashCode(): Int {
            return if (isNotEmpty()) {
                (size * 347 + this[0].hashCode()) * 347 + this[size - 1].hashCode()
            } else {
                0
            }
        }

        companion object {
            fun <T> Collection<T>.toDataBucketList() = DataBucketList(this)
        }
    }
}
