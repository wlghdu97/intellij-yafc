package com.xhlab.yafc.parser.data.deserializer

class DataBucket<TKey, TValue> {

    private val storage = hashMapOf<TKey, List<TValue>>()
    private val def = arrayListOf<TValue>()
    private var sealed = false

    // Replaces lists in storage with arrays. List with same contents get replaced with the same arrays
    fun sealAndDeduplicate(addExtra: List<TValue>? = null, sort: Comparator<TValue>? = null) {
        if (addExtra != null) {
            def.clear()
            def.addAll(addExtra)
        }

        val mapDict = hashMapOf<List<TValue>, List<TValue>>()
        for ((key, value) in storage.entries) {
            val list = value as? List<TValue> ?: continue
            val prev = mapDict[list]
            if (prev != null) {
                storage[key] = prev
            } else {
                val mergedList = if (addExtra == null) list else list + addExtra
                val result = if (sort != null && mergedList.size > 1) {
                    mergedList.sortedWith(sort)
                } else {
                    mergedList
                }

                mapDict[list] = result
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
}
