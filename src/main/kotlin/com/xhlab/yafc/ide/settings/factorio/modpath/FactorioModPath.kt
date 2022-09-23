package com.xhlab.yafc.ide.settings.factorio.modpath

import com.intellij.openapi.util.io.FileUtil
import com.xhlab.yafc.ide.YAFCBundle
import java.io.File

data class FactorioModPath(private val path: String) {
    /**
     * mod path in application directory of factorio
     * @see com.xhlab.yafc.ide.settings.factorio.path.FactorioPath.file
     */
    val file = File(path)
    val systemIndependentPath = FileUtil.toSystemIndependentName(path)
    val presentableName = FileUtil.toSystemDependentName(path)

    fun validate(): String? {
        return if (file.isDirectory) {
            val modList = File(file, "mod-list.json")
            if (modList.isFile) {
                null
            } else {
                YAFCBundle.message("yafc.factorio.no.mod.list.json.dialog.message")
            }
        } else {
            YAFCBundle.message("yafc.factorio.mod.path.specified.path.correctly.dialog.message")
        }
    }

    fun toRef() = FactorioModPathRef.create(path)
}
