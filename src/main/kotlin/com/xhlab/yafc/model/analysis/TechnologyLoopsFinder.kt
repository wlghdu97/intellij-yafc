package com.xhlab.yafc.model.analysis

import com.xhlab.yafc.model.data.Technology
import com.xhlab.yafc.model.data.YAFCDatabase
import com.xhlab.yafc.model.math.Graph
import com.xhlab.yafc.model.math.mergeStrongConnectedComponents

object TechnologyLoopsFinder {

    fun findTechnologyLoops(db: YAFCDatabase) {
        val graph = Graph<Technology>()
        for (technology in db.technologies.all) {
            for (prerequisite in technology.prerequisites) {
                graph.connect(prerequisite, technology)
            }
        }

        val merged = graph.mergeStrongConnectedComponents()
        var loops = false
        for (m in merged) {
            if (m.size > 1) {
                println("Technology loop: " + m.joinToString(", ") { it.locName })
                loops = true
            }
        }
        if (!loops) {
            println("No technology loops found")
        }
    }
}
