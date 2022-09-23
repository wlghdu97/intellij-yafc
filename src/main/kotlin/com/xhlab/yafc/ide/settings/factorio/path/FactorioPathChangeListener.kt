package com.xhlab.yafc.ide.settings.factorio.path

import com.intellij.util.messages.Topic

interface FactorioPathChangeListener {
    fun factorioPathChanged(path: FactorioPath?)

    companion object {
        val YAFC_FACTORIO_PATH_TOPIC = Topic.create(
            "com.xhlab.yafc.ide.settings.factorio.path.FactorioPathChangeListener",
            FactorioPathChangeListener::class.java,
            Topic.BroadcastDirection.NONE
        )
    }
}
