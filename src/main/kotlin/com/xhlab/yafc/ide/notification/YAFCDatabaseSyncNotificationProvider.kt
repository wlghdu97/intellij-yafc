package com.xhlab.yafc.ide.notification

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.model.YAFCProject
import java.util.function.Function
import javax.swing.JComponent

class YAFCDatabaseSyncNotificationProvider : EditorNotificationProvider, DumbAware {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> = Function<FileEditor, JComponent?> {
        if (file.isYafcFile()) {
            getNotificationPanel(project)
        } else {
            null
        }
    }

    private fun VirtualFile.isYafcFile(): Boolean {
        return isValid && extension == "yafc"
    }

    private fun getNotificationPanel(project: Project): EditorNotificationPanel? {
        val status = project.service<YAFCProject>()
        return when {
            (status.isSyncInProgress) -> DatabaseSyncNotification.Syncing.create(project)
            (status.lastSyncFailed) -> DatabaseSyncNotification.SyncFailed.create(project)
            (status.isSyncNeeded()) -> DatabaseSyncNotification.SyncNeeded.create(project)
            else -> null
        }
    }

    sealed class DatabaseSyncNotification {
        abstract fun create(project: Project): EditorNotificationPanel

        object SyncNeeded : DatabaseSyncNotification() {
            override fun create(project: Project): EditorNotificationPanel {
                return SyncEditorNotificationPanel().apply {
                    text = "YAFC db sync needed."
                    createActionLabel(YAFCBundle.message("yafc.sync")) {
                        project.service<YAFCProject>().syncDatabase()
                    }
                }
            }
        }

        object Syncing : DatabaseSyncNotification() {
            override fun create(project: Project): EditorNotificationPanel {
                return SyncEditorNotificationPanel().apply { text = "Syncing YAFC database..." }
            }
        }

        object SyncFailed : DatabaseSyncNotification() {
            override fun create(project: Project): EditorNotificationPanel {
                return SyncEditorNotificationPanel().apply {
                    text = "YAFC db sync failed."
                    createActionLabel(YAFCBundle.message("yafc.retry")) {
                        project.service<YAFCProject>().syncDatabase()
                    }
                }
            }
        }
    }

    private class SyncEditorNotificationPanel : EditorNotificationPanel(EditorColors.READONLY_BACKGROUND_COLOR)
}
