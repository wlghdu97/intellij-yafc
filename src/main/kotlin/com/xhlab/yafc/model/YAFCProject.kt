package com.xhlab.yafc.model

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.xhlab.yafc.ide.YAFCBundle
import com.xhlab.yafc.ide.notification.IntellijProgressTextIndicator
import com.xhlab.yafc.ide.project.sync.YAFCSyncListener
import com.xhlab.yafc.ide.settings.factorio.YAFCSettingsConfigurable
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPath
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathChangeListener
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathChangeListener.Companion.YAFC_FACTORIO_MOD_PATH_TOPIC
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathManager
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPath
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPathChangeListener
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPathChangeListener.Companion.YAFC_FACTORIO_PATH_TOPIC
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPathManager
import com.xhlab.yafc.ide.ui.IconCollection
import com.xhlab.yafc.model.analysis.TechnologyLoopsFinder
import com.xhlab.yafc.model.analysis.YAFCDependencies
import com.xhlab.yafc.model.analysis.factorio.*
import com.xhlab.yafc.model.data.YAFCDatabase
import com.xhlab.yafc.parser.FactorioDataSource
import com.xhlab.yafc.parser.ProgressTextIndicator
import com.xhlab.yafc.parser.YAFCLogger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class YAFCProject constructor(private val project: Project) :
    FactorioPathChangeListener, FactorioModPathChangeListener, Disposable {
    val yafcVersion: Version

    @Volatile
    var storage: YAFCStorage? = null
        private set

    val lastSyncFailed: Boolean
        get() = dbSyncState.isFailed
    val isSyncInProgress: Boolean
        get() = dbSyncState.isInProgress

    private val lock = ReentrantLock()

    private var dbSyncState: LastSyncState = LastSyncState.UNKNOWN
        get() = lock.withLock { return field }
        set(value) = lock.withLock { field = value }

    private var lastSyncedFactorioPath: FactorioPath? = null
    private var lastSyncedFactorioModPath: FactorioModPath? = null
    private var syncVariablesChanged = false

    private val connection = project.messageBus.connect()

    init {
        val versionString = PluginManagerCore.getPlugin(PluginId.getId("com.xhlab.yafc"))?.version ?: "0.0"
        yafcVersion = Version.fromString(versionString)

        connection.subscribe(YAFC_FACTORIO_PATH_TOPIC, this)
        connection.subscribe(YAFC_FACTORIO_MOD_PATH_TOPIC, this)
    }

    fun isSyncNeeded(): Boolean {
        return (project.service<YAFCProject>().storage == null || syncVariablesChanged)
    }

    fun syncDatabase() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Syncing YAFC database") {
            override fun run(indicator: ProgressIndicator) {
                val factorioPath = service<FactorioPathManager>().getProjectFactorioPathRef(project).resolve(project)
                if (factorioPath == null) {
                    updateSyncFailed()
                    fireProjectSyncFailureNotification(
                        YAFCBundle.message("yafc.no.factorio.path"),
                        createOpenSettingsAction()
                    )
                    return
                } else {
                    val errorMessage = factorioPath.validate()
                    if (errorMessage != null) {
                        updateSyncFailed()
                        fireProjectSyncFailureNotification(errorMessage, createOpenSettingsAction())
                        return
                    }
                }
                val modPath = service<FactorioModPathManager>().getProjectFactorioModPathRef(project).resolve(project)
                if (modPath != null) {
                    val errorMessage = modPath.validate()
                    if (errorMessage != null) {
                        updateSyncFailed()
                        fireProjectSyncFailureNotification(errorMessage, createOpenSettingsAction())
                        return
                    }
                }
                val progress = IntellijProgressTextIndicator(indicator)
                val logger = project.service<YAFCLogger>()
                val dataSource = FactorioDataSource(progress, logger)

                IconCollection.resetIconCache()

                syncVariablesChanged = false
                updateSyncStarted()

                val database = dataSource.parse(
                    factorioDataPath = factorioPath.dataFile.path,
                    modPath = modPath?.file?.path,
                    expensive = false,
                    locale = "en",
                    yafcVersion = yafcVersion
                )
                progress.setText("Post-processing", "Calculating dependencies")
                val dependencies = YAFCDependencies(database)
                TechnologyLoopsFinder.findTechnologyLoops(database)
                progress.setText("Post-processing", "Creating project")
                val errorCollector = project.service<ErrorCollector>()
                val analyses = createAndProcessAnalyses(database, dependencies, progress, errorCollector)
                storage = YAFCStorage(database, dependencies, analyses, dataSource)

                // write newly populated milestones to settings if first sync
                val milestones = analyses.get<FactorioMilestones>(FactorioAnalysisType.MILESTONES)
                if (milestones != null) {
                    writeMilestonesToSettingsIfEmpty(milestones)
                }
                setDefaultsToPreferencesIfNull(database)

                errorCollector.flush()
                updateSyncSucceeded()
            }

            override fun onThrowable(error: Throwable) {
                updateSyncFailed()
                fireProjectSyncFailureNotification(error.localizedMessage)
            }
        })
    }

    private fun createAndProcessAnalyses(
        database: YAFCDatabase,
        dependencies: YAFCDependencies,
        progress: ProgressTextIndicator,
        errorCollector: ErrorCollector
    ): FactorioAnalyses {
        return FactorioAnalyses().apply {
            val milestones = FactorioMilestones(database, dependencies)
            val automation = FactorioAutomationAnalysis(database, dependencies, milestones)
            val cost = FactorioCostAnalysis(database, milestones, automation, false)
            val currentMilestoneCost = FactorioCostAnalysis(database, milestones, automation, true)
            val technologyScience = FactorioTechnologyScienceAnalysis(database, dependencies, milestones)
            registerAnalysis(milestones, emptyList())
            registerAnalysis(automation, listOf(milestones))
            registerAnalysis(cost, listOf(milestones, automation))
            registerAnalysis(currentMilestoneCost, listOf(milestones, automation))
            registerAnalysis(technologyScience, listOf(milestones))
            processAnalyses(project.service(), progress, errorCollector)
        }
    }

    fun recomputeAnalysis(type: FactorioAnalysisType) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Recomputing YAFC Analyses") {
            override fun run(indicator: ProgressIndicator) {
                val progress = IntellijProgressTextIndicator(indicator)
                val errorCollector = project.service<ErrorCollector>()
                storage?.analyses?.recompute(type, project.service(), progress, errorCollector)
                errorCollector.flush()
            }
        })
    }

    private fun writeMilestonesToSettingsIfEmpty(milestones: FactorioMilestones) {
        val settings = project.service<YAFCProjectSettings>()
        if (settings.milestones.isEmpty()) {
            settings.setMilestones(milestones.currentMilestones.map { it.typeDotName to false })
        }
    }

    private fun setDefaultsToPreferencesIfNull(db: YAFCDatabase) {
        val pref = project.service<YAFCProjectPreferences>()
        if (pref.defaultBelt == null) {
            pref.defaultBelt = db.allBelts.minBy { it.beltItemsPerSecond }
        }
        if (pref.defaultInserter == null) {
            pref.defaultInserter = db.allInserters.sortedBy { it.energy?.type }.minBy { 1f / it.inserterSwingTime }
        }
    }

    private fun syncPublisher(block: YAFCSyncListener.() -> Unit) {
        block.invoke(project.messageBus.syncPublisher(YAFC_SYNC_TOPIC))
    }

    private fun updateSyncStarted() {
        dbSyncState = LastSyncState.IN_PROGRESS
        syncPublisher { syncStarted(project) }
    }

    private fun updateSyncSucceeded() {
        dbSyncState = LastSyncState.SUCCEEDED
        syncPublisher { syncSucceeded(project) }
    }

    private fun updateSyncFailed() {
        dbSyncState = LastSyncState.FAILED
        syncPublisher { syncFailed(project) }
    }

    private fun fireProjectSyncFailureNotification(content: String, action: AnAction? = null) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(YAFC_SYNC_NOTIFICATION_GROUP)
            .createNotification(YAFCBundle.message("yafc.project.sync.failed"), content, NotificationType.ERROR)
            .apply {
                if (action != null) {
                    addAction(action)
                }
            }
            .notify(project)
    }

    private fun createOpenSettingsAction(): NotificationAction {
        return NotificationAction.createSimple(YAFCBundle.message("yafc.open.settings")) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, YAFCSettingsConfigurable::class.java)
        }
    }

    override fun factorioPathChanged(path: FactorioPath?) {
        if (lastSyncedFactorioPath != path) {
            lastSyncedFactorioPath = path
            syncVariablesChanged = true
            syncPublisher { syncNeeded(project) }
        }
    }

    override fun factorioModPathChanged(modPath: FactorioModPath?) {
        if (lastSyncedFactorioModPath != modPath) {
            lastSyncedFactorioModPath = modPath
            syncVariablesChanged = true
            syncPublisher { syncNeeded(project) }
        }
    }

    override fun dispose() {
        connection.disconnect()
    }

    private enum class LastSyncState(
        val isInProgress: Boolean = false,
        val isSuccessful: Boolean = false,
        val isFailed: Boolean = false
    ) {
        UNKNOWN,
        IN_PROGRESS(isInProgress = true),
        SUCCEEDED(isSuccessful = true),
        FAILED(isFailed = true);

        init {
            assert(!(isSuccessful && isFailed))
        }
    }

    companion object {
        private const val YAFC_SYNC_NOTIFICATION_GROUP = "YAFC Project Sync"

        val YAFC_SYNC_TOPIC = Topic.create(
            "com.xhlab.yafc.ide.project.sync.YAFCSyncListener",
            YAFCSyncListener::class.java,
            Topic.BroadcastDirection.NONE
        )
    }

//    companion object {
//        private fun createProjectGson(db: Database) = Gson().newBuilder()
//            .registerTypeAdapter(YAFCProject::class.java, ProjectInstanceCreator(db))
//            .registerTypeAdapter(Guid::class.java, GuidTypeAdapter())
//            .create()
//
//        fun readFromFile(database: Database, path: String): YAFCProject {
//            val pathFile = File(path)
//            val project = if (path.isNotBlank() && pathFile.exists()) {
//                createProjectGson(database).fromJson(pathFile.reader(), YAFCProject::class.java).apply {
//                    if (yafcVersion != currentYafcVersion) {
//                        if (yafcVersion > currentYafcVersion) {
//                            collector.Error(
//                                "This file was created with future YAFC version. This may lose data.",
//                                ErrorSeverity.Important
//                            )
//                        }
//                    }
//                }
//            } else {
//                YAFCProject(database)
//            }.apply {
//                attachedFileName = path
//            }
//            return project
//        }
//    }
}
