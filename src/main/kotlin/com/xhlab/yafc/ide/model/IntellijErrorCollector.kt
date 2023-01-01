package com.xhlab.yafc.ide.model

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.xhlab.yafc.model.ErrorCollector
import com.xhlab.yafc.model.ErrorSeverity

class IntellijErrorCollector constructor(private val project: Project) : ErrorCollector() {

    override fun appendError(message: String, severity: ErrorSeverity) {
        super.appendError(message, severity)
        if (logger.isDebugEnabled) {
            logger.error(message)
        }
    }

    override fun appendException(throwable: Throwable, message: String, severity: ErrorSeverity) {
        super.appendException(throwable, message, severity)
        if (logger.isDebugEnabled) {
            logger.error(throwable.stackTraceToString())
        }
    }

    override fun flush() {
        if (allErrors.isEmpty()) {
            reset()
            return
        }

        val header = when {
            (severity == ErrorSeverity.CRITICAL) -> "Loading failed"
            (severity >= ErrorSeverity.MINOR_DATA_LOSS) -> "Loading completed with errors"
            else -> "Analysis warnings"
        }
        val type = if (severity >= ErrorSeverity.MINOR_DATA_LOSS) {
            NotificationType.ERROR
        } else {
            NotificationType.WARNING
        }
        NotificationGroupManager.getInstance()
            .getNotificationGroup("YAFC Project Sync")
            .createNotification(header, getArrErrors().joinToString("\n\n") { it.message }, type)
            .notify(project)

        reset()
    }

    companion object {
        private val logger = Logger.getInstance(IntellijErrorCollector::class.java)
    }
}
