package com.xhlab.yafc.ide.settings.factorio.modpath

import com.intellij.util.messages.Topic

interface FactorioModPathChangeListener {
    fun factorioModPathChanged(modPath: FactorioModPath?)

    companion object {
        val YAFC_FACTORIO_MOD_PATH_TOPIC = Topic.create(
            "com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathChangeListener",
            FactorioModPathChangeListener::class.java,
            Topic.BroadcastDirection.NONE
        )
    }
}
