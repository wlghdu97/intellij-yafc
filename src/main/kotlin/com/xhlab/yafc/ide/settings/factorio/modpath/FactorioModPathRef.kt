package com.xhlab.yafc.ide.settings.factorio.modpath

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

data class FactorioModPathRef(val referenceName: String) {

    fun isProjectRef() = (referenceName == PROJECT_FACTORIO_MOD_PATH_REF)

    fun resolve(project: Project): FactorioModPath? {
        val ref = dereferenceIfProject(project)
        return service<FactorioModPathManager>().resolveReference(ref.referenceName)
    }

    private fun dereferenceIfProject(project: Project): FactorioModPathRef {
        return if (isProjectRef()) {
            service<FactorioModPathManager>().getProjectFactorioModPathRef(project)
        } else {
            this
        }
    }

    companion object {
        const val PROJECT_FACTORIO_MOD_PATH_REF = "project"

        fun create(referenceName: String): FactorioModPathRef {
            return FactorioModPathRef(StringUtil.notNullize(referenceName, PROJECT_FACTORIO_MOD_PATH_REF))
        }
    }
}
