package com.xhlab.yafc

import com.google.ortools.Loader
import com.intellij.openapi.diagnostic.Logger
import com.intellij.testFramework.TestLoggerFactory
import com.xhlab.yafc.model.ProjectPerItemFlag
import com.xhlab.yafc.model.ProjectPerItemFlags
import com.xhlab.yafc.model.Version
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.analysis.*
import com.xhlab.yafc.model.analysis.factorio.*
import com.xhlab.yafc.model.util.toSet
import com.xhlab.yafc.parser.FactorioDataSource
import com.xhlab.yafc.parser.ParserProgressChangeListener
import java.util.*

fun main(args: Array<String>) {
    Loader.loadNativeLibraries()

    val factorioPath = args[0]
    val modPath = args[1]

    Logger.setFactory(TestLoggerFactory::class.java)

    StorageExecutor(factorioPath, modPath).run()
}

class StorageExecutor(
    private val factorioPath: String,
    private val modPath: String
) : ParserProgressChangeListener {
    private val yafcVersion = Version(0, 4, 0)
    private val indicator = TestProgressIndicator()
    private val errorCollector = TestErrorCollector()
    private val logger = TestLogger()
    private val dataSource = FactorioDataSource(indicator, logger)

    fun run() {
        val db = dataSource.parse(factorioPath, modPath, false, "en", yafcVersion)
        val dependencies = YAFCDependencies(db)
        TechnologyLoopsFinder.findTechnologyLoops(db)
        val settings = FakeYAFCProjectSettings()
        FactorioAnalyses().apply {
            val milestones = FactorioMilestones(db, dependencies)
            val automation = FactorioAutomationAnalysis(db, dependencies, milestones)
            val cost = FactorioCostAnalysis(db, milestones, automation, false)
            val currentMilestoneCost = FactorioCostAnalysis(db, milestones, automation, true)
            val technologyScience = FactorioTechnologyScienceAnalysis(db, dependencies, milestones)
            registerAnalysis(milestones, emptyList())
            registerAnalysis(automation, listOf(milestones))
            registerAnalysis(cost, listOf(milestones, automation))
            registerAnalysis(currentMilestoneCost, listOf(milestones, automation))
            registerAnalysis(technologyScience, listOf(milestones))
            processAnalyses(settings, indicator, errorCollector)
        }
        errorCollector.flush()
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

class FakeYAFCProjectSettings : YAFCProjectSettings {
    override val milestones: List<String> = listOf(
        "Item.automation-science-pack",
        "Item.logistic-science-pack",
        "Item.military-science-pack",
        "Item.chemical-science-pack",
        "Item.py-science-pack",
        "Item.production-science-pack",
        "Item.utility-science-pack",
        "Item.space-science-pack"
    )
    override val itemFlags: SortedMap<String, ProjectPerItemFlags> = TreeMap() // TODO: add comparator
    override val miningProductivity: Float = 0f
    override var reactorSizeX: Float = 2f
    override var reactorSizeY: Float = 2f

    init {
        itemFlags[milestones[0]] = ProjectPerItemFlag.MILESTONE_UNLOCKED.toSet()
    }

    override fun setMilestones(newMilestones: List<Pair<String, Boolean>>) {
        throw NotImplementedError()
    }

    override fun setFlag(itemKey: String, flag: ProjectPerItemFlags, set: Boolean) {
        throw NotImplementedError()
    }
}
