package com.xhlab.yafc.ide.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.xhlab.yafc.parser.FactorioDataSource
import com.xhlab.yafc.parser.ParserProgressChangeListener
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class ParserAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ParserDialog(project).showAndGet()
    }

    class ParserDialog constructor(project: Project) : DialogWrapper(project), ParserProgressChangeListener {

        private val label: JBLabel by lazy { JBLabel() }
        private val dataSource = project.service<FactorioDataSource>().apply {
            progressListener = this@ParserDialog
        }

        private val factorioPath = ""
        private val modPath = ""
        private val projectPath = ""

        init {
            title = "Parsing"
            init()
        }

        override fun init() {
            super.init()
            ApplicationManager.getApplication().executeOnPooledThread {
                dataSource.parse(factorioPath, modPath, projectPath, false, "en")
            }
        }

        override fun createCenterPanel(): JComponent {
            return JPanel(BorderLayout()).apply {
                label.preferredSize = Dimension(320, 100)
                add(label, BorderLayout.CENTER)
            }
        }

        override fun progressChanged(title: String, description: String) {
            this@ParserDialog.title = title
            this@ParserDialog.label.text = description
        }

        override fun currentLoadingModChanged(mod: String?) {
            if (mod != null) {
                this@ParserDialog.label.text = "Loading mod : $mod"
            }
        }
    }
}
