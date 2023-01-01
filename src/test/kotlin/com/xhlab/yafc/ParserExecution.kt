package com.xhlab.yafc

import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.TestLoggerFactory
import com.xhlab.yafc.model.Version
import com.xhlab.yafc.parser.FactorioDataSource
import com.xhlab.yafc.parser.ParserProgressChangeListener

fun main(args: Array<String>) {
    val factorioPath = args[0]
    val modPath = args[1]

    Logger.setFactory(TestLoggerFactory::class.java)

    ParserExecutor(factorioPath, modPath).run()
}

class ParserExecutor(
    private val factorioPath: String,
    private val modPath: String,
) : ParserProgressChangeListener {
    private val yafcVersion = Version(0, 4, 0)
    private val indicator = TestProgressIndicator()
    private val logger = TestLogger()
    private val dataSource = FactorioDataSource(indicator, logger)

    fun run() {
        dataSource.parse(factorioPath, modPath, false, "en", yafcVersion)
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
