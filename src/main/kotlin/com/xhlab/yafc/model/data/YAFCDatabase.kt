package com.xhlab.yafc.model.data

class YAFCDatabase constructor(
    val rootAccessible: List<FactorioObject>,
    val allSciencePacks: List<Item>,
    val objectsByTypeName: Map<String, FactorioObject>,
    val fluidVariants: Map<String, List<Fluid>>,
    val voidEnergy: Goods,
    val electricity: Goods,
    val electricityGeneration: Recipe,
    val heat: Goods,
    val character: Entity,
    val allCrafters: List<EntityCrafter>,
    val allModules: List<Item>,
    val allBeacons: List<EntityBeacon>,
    val allBelts: List<EntityBelt>,
    val allInserters: List<EntityInserter>,
    val allAccumulators: List<EntityAccumulator>,
    val allContainers: List<EntityContainer>,
    val objects: FactorioIdRange<FactorioObject>,
    val goods: FactorioIdRange<Goods>,
    val specials: FactorioIdRange<Special>,
    val items: FactorioIdRange<Item>,
    val fluids: FactorioIdRange<Fluid>,
    val recipes: FactorioIdRange<Recipe>,
    val mechanics: FactorioIdRange<Mechanics>,
    val recipesAndTechnologies: FactorioIdRange<RecipeOrTechnology>,
    val technologies: FactorioIdRange<Technology>,
    val entities: FactorioIdRange<Entity>
) {
    fun findClosestVariant(id: String): FactorioObject? {
        val splitter = id.indexOf("@")
        val (baseId, temperature) = if (splitter >= 0) {
            val baseId = id.substring(0, splitter)
            val temperature = id.substring(splitter + 1).toInt()
            baseId to temperature
        } else {
            id to 0
        }

        val result = objectsByTypeName[baseId]
        if (result != null) {
            return result
        }

        val variants = fluidVariants[baseId]
        if (variants != null) {
            var prev = variants[0]
            for (i in 1 until variants.size) {
                val cur = variants[i]
                if (cur.temperature >= temperature) {
                    return if (cur.temperature - temperature > temperature - prev.temperature) prev else cur
                }
                prev = cur
            }
            return prev
        }

        return null
    }

    companion object {
        var constantCombinatorCapacity = 18
    }
}

// Since Factorio objects are sorted by type, objects of one type always occupy continuous range.
// Because of that we can replace Dictionary<SomeFactorioObjectSubtype, T> with just plain array indexed with object id with range offset
// This is mostly useful for analysis algorithms that needs a bunch of these constructs
class FactorioIdRange<T : FactorioObject>(
    internal val start: Int,
    end: Int,
    val source: List<FactorioObject>
) {
    val size = end - start

    @Suppress("UNCHECKED_CAST")
    val all = source.subList(start, end).map { it as T }

    operator fun get(index: Int): T = all[index]

    operator fun get(id: FactorioId): T = all[id.id - start]

    fun <TValue> createMapping() = Mapping<T, TValue>(this)

    fun <TOther : FactorioObject, TValue> createDoubleMapping(
        other: FactorioIdRange<TOther>,
        mapFunc: (T, TOther) -> TValue
    ) = DoubleMapping(this, other, mapFunc)

    fun <TValue> createDoubleMapping(
        mapFunc: (T, T) -> TValue
    ) = DoubleMapping(this, this, mapFunc)

    fun <TValue> createMapping(mapFunc: (T) -> TValue): Mapping<T, TValue> {
        val map = createMapping<TValue>()
        for (o in all) {
            map[o] = mapFunc(o)
        }
        return map
    }
}

// Mapping[TKey, TValue] is almost like a dictionary where TKey is FactorioObject but it is an array wrapper and therefore very fast. This is preferable way to add custom properties to FactorioObjects
class Mapping<TKey : FactorioObject, TValue> internal constructor(
    private val source: FactorioIdRange<TKey>,
) : MutableMap<TKey, TValue?> {
    private val offset: Int = source.start

    @Suppress("UNCHECKED_CAST")
    private val data = arrayOfNulls<Any>(source.size) as Array<TValue?>

    override val size: Int
        get() = data.size

    override fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    override fun containsKey(key: TKey): Boolean {
        return true
    }

    override fun containsValue(value: TValue?): Boolean {
        return data.contains(value)
    }

    override operator fun get(key: TKey): TValue? = data[key.id.id - offset]

    operator fun get(id: FactorioId): TValue? = data[id.id - offset]

    override fun put(key: TKey, value: TValue?): TValue? {
        val idx = key.id.id - offset
        val prev = data[idx]
        data[idx] = value
        return prev
    }

    operator fun set(id: FactorioId, value: TValue?) {
        data[id.id - offset] = value
    }

    operator fun set(key: TKey, value: TValue?) {
        put(key, value)
    }

    override fun remove(key: TKey): TValue? {
        throw RuntimeException("remove in Mapping is not supported")
    }

    override fun remove(key: TKey, value: TValue?): Boolean {
        remove(key)
        return false
    }

    override fun putAll(from: Map<out TKey, TValue?>) {
        from.entries.forEach { (k, v) ->
            put(k, v)
        }
    }

    override fun clear() {
        fill(null)
    }

    fun fill(value: TValue?) {
        data.fill(value)
    }

    override val keys: MutableSet<TKey>
        get() = source.all.toMutableSet()

    override val values: MutableCollection<TValue?>
        get() = data.toMutableList()

    override val entries: MutableSet<MutableMap.MutableEntry<TKey, TValue?>>
        get() = source.all.asSequence()
            .mapIndexed { idx, key ->
                object : MutableMap.MutableEntry<TKey, TValue?> {
                    override val key: TKey = key
                    override val value: TValue? = data[idx]
                    override fun setValue(newValue: TValue?): TValue? {
                        return put(key, newValue)
                    }
                }
            }.toMutableSet()
}

class DoubleMapping<TKey1 : FactorioObject, TKey2 : FactorioObject, TValue> constructor(
    key1: FactorioIdRange<TKey1>,
    key2: FactorioIdRange<TKey2>,
    mapFunc: (TKey1, TKey2) -> TValue
) {
    private val offset1 = key1.start
    private val offset2 = key2.start
    private val count1 = key1.size
    private val count2 = key2.size

    @Suppress("UNCHECKED_CAST")
    private val data = Array(count1 * count2) {
        val x = it / count1
        val y = it % count2
        mapFunc(key1[x], key2[y]) as Any
    } as Array<TValue?>

    operator fun get(pair: Pair<TKey1, TKey2>): TValue? =
        data[(pair.first.id.id - offset1) * count1 + (pair.second.id.id - offset2)]

    operator fun set(pair: Pair<TKey1, TKey2>, value: TValue?) {
        data[(pair.first.id.id - offset1) * count1 + (pair.second.id.id - offset2)] = value
    }

    fun copyRow(from: TKey1, to: TKey1) {
        if (from == to) {
            return
        }
        val fromId = (from.id.id - offset1) * count1
        val toId = (to.id.id - offset1) * count1
        data.copyInto(data, toId, fromId, fromId + count1)
    }
}
