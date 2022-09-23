package com.xhlab.yafc.ide.project.sync

import com.intellij.openapi.project.Project

interface YAFCSyncListener {
    fun syncNeeded(project: Project)
    fun syncStarted(project: Project)
    fun syncSucceeded(project: Project)
    fun syncFailed(project: Project)
}
