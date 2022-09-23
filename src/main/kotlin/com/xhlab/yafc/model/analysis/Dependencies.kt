package com.xhlab.yafc.model.analysis

import com.xhlab.yafc.model.data.FactorioId
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.Mapping
import com.xhlab.yafc.model.data.YAFCDatabase

class Dependencies constructor(db: YAFCDatabase) {
    val dependencyList: Mapping<FactorioObject, List<DependencyList>>
    val reverseDependencies: Mapping<FactorioObject, MutableList<FactorioId>>

    init {
        dependencyList = db.objects.createMapping()
        reverseDependencies = db.objects.createMapping()
        for (obj in db.objects.all) {
            reverseDependencies[obj] = arrayListOf()
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

        override fun addId(raw: List<FactorioId>, flags: DependencyList.Flags) {
            list.add(DependencyList(flags, raw.toTypedArray()))
        }

        override fun addObject(raw: List<FactorioObject>, flags: DependencyList.Flags) {
            val elems = Array(raw.size) { raw[it].id }
            list.add(DependencyList(flags, elems))
        }

        fun pack(): List<DependencyList> {
            return list.apply {
                clear()
            }
        }
    }
}
