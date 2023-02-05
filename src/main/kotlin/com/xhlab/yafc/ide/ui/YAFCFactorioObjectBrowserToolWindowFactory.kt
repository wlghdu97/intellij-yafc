package com.xhlab.yafc.ide.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.xhlab.yafc.ide.YAFCBundle

class YAFCFactorioObjectBrowserToolWindowFactory : ToolWindowFactory {

    override fun init(toolWindow: ToolWindow) {
        super.init(toolWindow)
        toolWindow.stripeTitle = YAFCBundle.message("toolwindow.stripe.YAFCFactorioObjectBrowser")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(YAFCFactorioObjectBrowser(project), null, false)
        contentManager.addContent(content)
    }
}
