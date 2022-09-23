package com.xhlab.yafc.ide.settings.factorio.path

import com.intellij.openapi.util.SystemInfo
import java.io.File

object FactorioPathUtil {
    private val FACTORIO_BASE_NAME = when {
        (SystemInfo.isWindows) -> "factorio.exe"
        (SystemInfo.isMac) -> "factorio.app"
        else -> "factorio"
    }

    fun detectAllFactorioPaths(): List<File> {
        val paths = arrayListOf<File>()
        addFactorioFromApplication(paths)
        return paths
    }

    private fun addFactorioFromApplication(list: MutableList<File>) {
        if (SystemInfo.isMac) {
            val application = File("/Applications", FACTORIO_BASE_NAME)
            if (application.exists() && application.canExecute()) {
                list.add(application)
            }
        }
    }
}
