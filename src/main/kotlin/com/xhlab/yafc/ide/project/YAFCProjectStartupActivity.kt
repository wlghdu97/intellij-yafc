package com.xhlab.yafc.ide.project

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.xhlab.yafc.model.YAFCProject

class YAFCProjectStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        val yafcProject = project.service<YAFCProject>()
        if (yafcProject.isSyncNeeded()) {
            yafcProject.syncDatabase()
        }
    }
}
