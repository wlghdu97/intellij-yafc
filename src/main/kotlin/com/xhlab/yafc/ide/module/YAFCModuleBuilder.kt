package com.xhlab.yafc.ide.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel

class YAFCModuleBuilder : ModuleBuilder() {

    override fun getModuleType(): ModuleType<*> = YAFCModuleType.INSTANCE

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep? {
        return super.getCustomOptionsStep(context, parentDisposable)
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
    }
}
