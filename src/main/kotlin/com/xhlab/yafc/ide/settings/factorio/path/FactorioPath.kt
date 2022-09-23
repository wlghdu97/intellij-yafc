package com.xhlab.yafc.ide.settings.factorio.path

import com.intellij.openapi.components.service
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.text.SemVer
import com.xhlab.yafc.ide.YAFCBundle
import java.io.File

data class FactorioPath(private val path: String) {
    /**
     * application directory of Factorio
     * @see <a href="https://wiki.factorio.com/Application_directory">Factorio application directory</a>
     */
    val file = File(path)
    val systemIndependentPath = FileUtil.toSystemIndependentName(path)
    val presentableName = FileUtil.toSystemDependentName(path)

    val dataFile = File(path, if (SystemInfo.isMac) "Contents/data" else "data")

    private val rootFileValid = if (SystemInfo.isMac) {
        (file.isDirectory && file.extension == "app")
    } else {
        (file.isDirectory)
    }

    private val isValid = (rootFileValid && dataFile.isDirectory)

    fun getCachedVersion(): Ref<SemVer>? {
        return service<FactorioPathManager>().getCachedVersion(this)
    }

    fun validate(): String? {
        return if (isValid) {
            null
        } else {
            YAFCBundle.message("yafc.factorio.path.specified.path.correctly.dialog.message")
        }
    }

    fun fetchVersion(afterFetch: (SemVer?) -> Unit) {
        service<FactorioPathManager>().fetchVersion(this, afterFetch)
    }

    fun toRef() = FactorioPathRef.create(path)
}
