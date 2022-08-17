package com.xhlab.yafc

import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.TestLoggerFactory
import com.xhlab.yafc.parser.FactorioDataSource
import com.xhlab.yafc.parser.ParserProgressChangeListener

fun main(args: Array<String>) {
    val factorioPath = args[0]
    val modPath = args[1]
    val projectPath = args[2]

    Logger.setFactory(TestLoggerFactory::class.java)

    ParserExecutor(factorioPath, modPath, projectPath).run()
}

class ParserExecutor(
    private val factorioPath: String,
    private val modPath: String,
    private val projectPath: String
) : ParserProgressChangeListener {

    private val dataSource = FactorioDataSource().apply {
        progressListener = this@ParserExecutor
    }

    fun run() {
        dataSource.parse(factorioPath, modPath, projectPath, false, "en", false)
    }

    override fun progressChanged(title: String, description: String) {
        println("$title : $description")
    }

    override fun currentLoadingModChanged(mod: String?) {
        if (mod != null) {
            println("Loading mod : $mod")
        }
    }
}
