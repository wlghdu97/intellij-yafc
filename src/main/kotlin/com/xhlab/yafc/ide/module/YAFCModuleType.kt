package com.xhlab.yafc.ide.module

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import javax.swing.Icon

class YAFCModuleType : ModuleType<YAFCModuleBuilder>(MODULE_ID) {

    override fun createModuleBuilder() = YAFCModuleBuilder()

    override fun getName() = "YAFC"

    override fun getDescription() = "YAFC module"

    override fun getNodeIcon(isOpened: Boolean): Icon = AllIcons.General.GearPlain

    companion object {
        const val MODULE_ID = "YAFC_MODULE"
        val INSTANCE by lazy { ModuleTypeManager.getInstance().findByID(MODULE_ID) as YAFCModuleType }
    }
}
