package com.xhlab.yafc.model.analysis

import com.xhlab.yafc.model.data.Database
import com.xhlab.yafc.model.data.Technology
import com.xhlab.yafc.model.math.Graph

object TechnologyLoopsFinder {

    fun findTechnologyLoops(db: Database) {
        val graph = Graph<Technology>()
        for (technology in db.technologies.all) {
            for (prerequisite in technology.prerequisites) {
                graph.connect(prerequisite, technology)
            }
        }

        val merged = graph.mergeStrongConnectedComponents()
        var loops = false
        for (m in merged) {
            if (m.isNotEmpty()) {
                println("Technology loop: " + m.joinToString(", ") { it.locName })
                loops = true
            }
        }
        if (!loops) {
            println("No technology loops found")
        }
    }
}
