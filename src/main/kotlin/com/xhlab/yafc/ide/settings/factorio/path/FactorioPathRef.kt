package com.xhlab.yafc.ide.settings.factorio.path

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

data class FactorioPathRef(val referenceName: String) {

    fun isProjectRef() = (referenceName == PROJECT_FACTORIO_PATH_REF)

    fun resolve(project: Project): FactorioPath? {
        val ref = dereferenceIfProject(project)
        return service<FactorioPathManager>().resolveReference(ref.referenceName)
    }

    private fun dereferenceIfProject(project: Project): FactorioPathRef {
        return if (isProjectRef()) {
            service<FactorioPathManager>().getProjectFactorioPathRef(project)
        } else {
            this
        }
    }

    companion object {
        const val PROJECT_FACTORIO_PATH_REF = "project"

        fun create(referenceName: String): FactorioPathRef {
            return FactorioPathRef(StringUtil.notNullize(referenceName, PROJECT_FACTORIO_PATH_REF))
        }
    }
}
