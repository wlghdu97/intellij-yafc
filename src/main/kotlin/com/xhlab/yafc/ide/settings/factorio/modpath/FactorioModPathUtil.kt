package com.xhlab.yafc.ide.settings.factorio.modpath

import com.intellij.openapi.util.SystemInfo
import java.io.File

object FactorioModPathUtil {

    fun detectAllFactorioModPaths(): List<File> {
        val paths = arrayListOf<File>()
        addFactorioFromApplication(paths)
        return paths
    }

    private fun addFactorioFromApplication(list: MutableList<File>) {
        if (SystemInfo.isMac) {
            val userHome = System.getProperty("user.home")
            val applicationSupportMod = File(userHome, "Library/Application Support/factorio/mods")
            if (applicationSupportMod.isDirectory) {
                list.add(applicationSupportMod)
            }
        }
    }
}
