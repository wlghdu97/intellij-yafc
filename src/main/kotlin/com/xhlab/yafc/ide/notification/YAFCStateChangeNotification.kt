package com.xhlab.yafc.ide.notification

import com.intellij.openapi.project.Project
import com.intellij.ui.AppUIUtil.invokeLaterIfProjectAlive
import com.intellij.ui.EditorNotifications
import com.xhlab.yafc.ide.project.sync.YAFCSyncListener

class YAFCStateChangeNotification : YAFCSyncListener {
    override fun syncNeeded(project: Project) = notify(project)
    override fun syncStarted(project: Project) = notify(project)
    override fun syncSucceeded(project: Project) = notify(project)
    override fun syncFailed(project: Project) = notify(project)

    private fun notify(project: Project) {
        invokeLaterIfProjectAlive(project) {
            val editorNotifications = EditorNotifications.getInstance(project)
            editorNotifications.updateAllNotifications()
        }
    }
}
