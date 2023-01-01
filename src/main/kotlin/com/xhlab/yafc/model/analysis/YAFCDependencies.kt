package com.xhlab.yafc.model.analysis

import com.xhlab.yafc.model.data.FactorioId
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.Mapping
import com.xhlab.yafc.model.data.YAFCDatabase

class YAFCDependencies constructor(db: YAFCDatabase) {
    val dependencyList: Mapping<FactorioObject, Array<DependencyList>>
    val reverseDependencies: Mapping<FactorioObject, HashSet<FactorioId>>

    init {
        dependencyList = db.objects.createMapping()
        reverseDependencies = db.objects.createMapping()
        for (obj in db.objects.all) {
            reverseDependencies[obj] = HashSet(0)
        }

        val collector = DependencyCollector()
        val temp = mutableListOf<FactorioObject>()
        for (obj in db.objects.all) {
            obj.getDependencies(collector, temp)
            val packed = collector.pack()
            dependencyList[obj] = packed

            for (group in packed) {
                for (req in group.elements) {
                    val reverseDependency = reverseDependencies[req]
                    if (reverseDependency != null && !reverseDependency.contains(obj.id)) {
                        reverseDependency.add(obj.id)
                    }
                }
            }
        }
    }

    private class DependencyCollector : IDependencyCollector {
        private val list = arrayListOf<DependencyList>()

        override fun addId(raw: List<FactorioId>, flags: DependencyListFlags) {
            list.add(DependencyList(flags, raw.toTypedArray()))
        }

        override fun addObject(raw: List<FactorioObject>, flags: DependencyListFlags) {
            val elems = Array(raw.size) { raw[it].id }
            list.add(DependencyList(flags, elems))
        }

        fun pack(): Array<DependencyList> {
            return list.toTypedArray().apply {
                list.clear()
            }
        }
    }
}
