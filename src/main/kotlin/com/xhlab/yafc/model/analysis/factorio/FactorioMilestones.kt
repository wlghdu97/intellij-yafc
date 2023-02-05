package com.xhlab.yafc.model.analysis.factorio

import com.intellij.openapi.diagnostic.Logger
import com.xhlab.yafc.model.ErrorCollector
import com.xhlab.yafc.model.ErrorSeverity
import com.xhlab.yafc.model.ProjectPerItemFlag
import com.xhlab.yafc.model.YAFCProjectSettings
import com.xhlab.yafc.model.analysis.DependencyList
import com.xhlab.yafc.model.analysis.YAFCDependencies
import com.xhlab.yafc.model.data.*
import com.xhlab.yafc.model.math.highestBitSet
import com.xhlab.yafc.model.util.*
import java.util.*

private typealias ProcessingFlags = EnumSet<ProcessingFlag>

private enum class ProcessingFlag(override val value: Int) : EnumFlag {
    IN_QUEUE(1 shl 0),
    INITIAL(1 shl 1),
    MILESTONE_NEED_ORDERING(1 shl 2),
    FORCE_INACCESSIBLE(1 shl 3);
}

class FactorioMilestones constructor(
    private val db: YAFCDatabase,
    private val dependencies: YAFCDependencies
) : FactorioAnalysis(FactorioAnalysisType.MILESTONES) {
    val currentMilestones = arrayListOf<FactorioObject>()

    // 1uL means it is accessible
    val milestoneResult: Mapping<FactorioObject, ULong> = db.objects.createMapping()
    var lockedMask: ULong = 0uL
        private set

    val highestMilestone: Mapping<FactorioObject, FactorioObject?> = db.objects.createMapping()

    // was part of AnalysisExtensions
    fun isAccessible(obj: FactorioObject): Boolean = milestoneResult[obj] != 0uL

    fun isAccessibleWithCurrentMilestones(id: FactorioId) = ((milestoneResult[id] ?: 0uL) and lockedMask) == 1uL

    fun isAccessibleWithCurrentMilestones(obj: FactorioObject) = ((milestoneResult[obj] ?: 0uL) and lockedMask) == 1uL

    fun isAccessibleAtNextMilestone(obj: FactorioObject): Boolean {
        val milestoneMask = (milestoneResult[obj] ?: 0uL) and lockedMask
        return when {
            (milestoneMask == 1uL) -> true
            ((milestoneMask and 1uL) != 0uL) -> false
            else -> (((milestoneMask - 1uL) and (milestoneMask - 2uL)) == 0uL) // milestoneMask is a power of 2 + 1
        }
    }

    private fun projectSettingsChanged(settings: YAFCProjectSettings, visualOnly: Boolean) {
        if (!visualOnly) {
            lockedMask = getLockedMaskFromProject(settings)
        }
    }

    override fun compute(settings: YAFCProjectSettings, errorCollector: ErrorCollector) {
        val userMilestones = settings.milestones
        if (userMilestones.isEmpty()) {
            // initial value
            computeWithParameters(db.allSciencePacks, settings, true, errorCollector)
        } else {
            val milestones = userMilestones.mapNotNull { typeDotName ->
                db.objects.all.find { it.typeDotName == typeDotName }.apply {
                    if (this == null) {
                        appendNotAccessibleMilestoneError(typeDotName, errorCollector)
                    }
                }
            }
            computeWithParameters(milestones, settings, false, errorCollector)
        }
    }

    private fun computeWithParameters(
        milestones: List<FactorioObject>,
        settings: YAFCProjectSettings,
        autoSort: Boolean,
        errorCollector: ErrorCollector
    ) {
        // reset result
        currentMilestones.clear()
        milestoneResult.fill(0uL)
        lockedMask = 0uL

        val time = System.currentTimeMillis()
        val processing = db.objects.createMapping<ProcessingFlags>()
        val processingQueue: Queue<FactorioId> = LinkedList()

        // set root items accessible
        for (rootAccessible in db.rootAccessible) {
            milestoneResult[rootAccessible] = 1uL
            processingQueue.add(rootAccessible.id)
            processing[rootAccessible] = ProcessingFlag.INITIAL or ProcessingFlag.IN_QUEUE
        }

        // apply user-defined item flags
        for ((typeDotName, flag) in settings.itemFlags) {
            val mappedObj = db.objects.all.find { it.typeDotName == typeDotName }
            if (mappedObj == null) {
                appendNotAccessibleMilestoneError(typeDotName, errorCollector)
                continue
            }
            if (DataUtils.hasFlags(flag, ProjectPerItemFlag.MARKED_ACCESSIBLE.toSet())) {
                milestoneResult[mappedObj] = 1uL
                processingQueue.add(mappedObj.id)
                processing[mappedObj] = ProcessingFlag.INITIAL or ProcessingFlag.IN_QUEUE
            } else if (DataUtils.hasFlags(flag, ProjectPerItemFlag.MARKED_INACCESSIBLE.toSet())) {
                processing[mappedObj] = ProcessingFlag.FORCE_INACCESSIBLE.toSet()
            }
        }

        if (autoSort) {
            // Adding default milestones AND special flag to auto-order them
            milestones.forEach { milestone ->
                processing[milestone] = processing[milestone] or ProcessingFlag.MILESTONE_NEED_ORDERING
            }
        } else {
            currentMilestones.addAll(milestones)
            milestones.forEachIndexed { index, milestone ->
                milestoneResult[milestone] = (1uL shl (index + 1)) or 1uL
            }
        }

        val dependencyList = dependencies.dependencyList
        val reverseDependencies = dependencies.reverseDependencies
        val milestonesNotReachable = arrayListOf<FactorioObject>()

        var nextMilestoneMask = 0x2uL
        var accessibleObjects = 0

        var flagMask = 0uL
        for (i in 0..milestones.size) {
            flagMask = flagMask or (1uL shl i)
            if (i > 0) {
                val milestone = currentMilestones.getOrNull(i - 1)
                if (milestone == null) {
                    for (pack in db.allSciencePacks) {
                        if (currentMilestones.indexOf(pack) == -1) {
                            currentMilestones.add(pack)
                            milestonesNotReachable.add(pack)
                        }
                    }
                    break
                }

                logger.info("Processing milestone ${milestone.locName}")
                processingQueue.add(milestone.id)
                processing[milestone] = ProcessingFlag.INITIAL or ProcessingFlag.IN_QUEUE
            }

            while (processingQueue.size > 0) {
                run {
                    val elem = processingQueue.remove()
                    val entry = dependencyList[elem] ?: emptyArray()

                    val cur = milestoneResult[elem] ?: 0uL
                    var eFlags = cur
                    val isInitial = (processing[elem] and ProcessingFlag.INITIAL).isNotEmpty()
                    processing[elem] = processing[elem] and ProcessingFlag.MILESTONE_NEED_ORDERING

                    for (list in entry) {
                        // if dependency list is ingredient or tech prerequisite
                        if ((list.flags.value and DependencyList.Flag.REQUIRE_EVERYTHING.value) != 0) {
                            for (req in list.elements) {
                                val reqFlags = milestoneResult[req] ?: 0uL
                                if (reqFlags == 0uL && !isInitial) {
                                    return@run
                                }
                                eFlags = eFlags or reqFlags
                            }
                        } else {
                            var groupFlags = 0uL
                            for (req in list.elements) {
                                val acc = milestoneResult[req] ?: 0uL
                                if (acc == 0uL) {
                                    continue
                                }
                                if (acc < groupFlags || groupFlags == 0uL) {
                                    groupFlags = acc
                                }
                            }

                            if (groupFlags == 0uL && !isInitial) {
                                return@run
                            }
                            eFlags = eFlags or groupFlags
                        }
                    }
                    if (!isInitial) {
                        if (eFlags == cur || (eFlags or flagMask) != flagMask) {
                            return@run
                        }
                    } else {
                        eFlags = eFlags and flagMask
                    }

                    accessibleObjects += 1
//                    val obj = db.objects[elem]
//                    println("Added object " + obj.name + " [" + obj.typeDotName + "] with mask " + eFlags + " (was " + cur + ")")

                    if (processing[elem] == ProcessingFlag.MILESTONE_NEED_ORDERING.toSet()) {
                        processing[elem] = EnumSet.noneOf(ProcessingFlag::class.java)
                        eFlags = eFlags or nextMilestoneMask
                        nextMilestoneMask = nextMilestoneMask shl 1
                        currentMilestones.add(db.objects[elem])
                    }

                    milestoneResult[elem] = eFlags

                    for (revDep in reverseDependencies[elem] ?: emptyList()) {
                        if ((processing[revDep] and ProcessingFlag.MILESTONE_NEED_ORDERING.inv()).isNotEmpty() ||
                            milestoneResult[revDep] != 0uL
                        ) {
                            continue
                        }
                        processing[revDep] = processing[revDep] or ProcessingFlag.IN_QUEUE
                        processingQueue.add(revDep)
                    }
                }
            }
        }

        // original setting-modifying code is moved to YAFCProject.

        lockedMask = getLockedMaskFromProject(settings)

        val hasAutomatableRocketLaunch = milestoneResult[db.objectsByTypeName["Special.launch"]] != 0uL
        when {
            (accessibleObjects < db.objects.size / 2) -> {
                errorCollector.appendError(
                    HalfInaccessible + MaybeBug + MilestoneAnalysisIsImportant + UseDependencyExplorer,
                    ErrorSeverity.ANALYSIS_WARNING
                )
            }

            (!hasAutomatableRocketLaunch) -> {
                errorCollector.appendError(
                    RocketLaunchInaccessible + MaybeBug + MilestoneAnalysisIsImportant + UseDependencyExplorer,
                    ErrorSeverity.ANALYSIS_WARNING
                )
            }

            (milestonesNotReachable.isNotEmpty()) -> {
                val locNames = milestonesNotReachable.joinToString(", ") { it.locName }
                errorCollector.appendError(
                    MilestonesNotReachable.format(locNames) + MaybeBug + MilestoneAnalysisIsImportant + UseDependencyExplorer,
                    ErrorSeverity.ANALYSIS_WARNING
                );
            }
        }

        // pre-calculate highest milestones of all
        db.objects.all.forEach {
            highestMilestone[it] = getHighest(it, true)
        }

        val elapsedTime = System.currentTimeMillis() - time
        logger.info("Milestones calculation finished in $elapsedTime ms.")
    }

    private fun getLockedMaskFromProject(settings: YAFCProjectSettings): ULong {
        var lockedMask = ULong.MAX_VALUE
        for ((index, milestone) in currentMilestones.withIndex()) {
            if (DataUtils.hasFlags(
                    settings.flags(milestone.typeDotName),
                    ProjectPerItemFlag.MILESTONE_UNLOCKED.toSet()
                )
            ) {
                lockedMask = lockedMask and (1uL shl (index + 1)).inv()
            }
        }
        return lockedMask
    }

    private fun appendNotAccessibleMilestoneError(typeDotName: String, errorCollector: ErrorCollector) {
        errorCollector.appendError(
            "There are some milestones that are not accessible: $typeDotName. You may remove these from milestone list," +
                    MaybeBug + MilestoneAnalysisIsImportant + UseDependencyExplorer,
            ErrorSeverity.ANALYSIS_WARNING
        )
    }

    fun getHighest(target: FactorioObject, all: Boolean): FactorioObject? {
        var ms = milestoneResult[target] ?: 0uL
        if (!all) {
            ms = ms and lockedMask
        }
        if (ms == 0uL) {
            return null
        }

        val msb = ms.highestBitSet - 1
        return if (msb < 0 || msb >= currentMilestones.size) {
            null
        } else {
            currentMilestones[msb]
        }
    }

    override val description =
        "Milestone analysis starts from objects that are placed on map by the map generator and tries to find all objects that are accessible from that, taking notes about which objects are locked behind which milestones."

    companion object {
        private val logger = Logger.getInstance(FactorioMilestones::class.java)

        private const val HalfInaccessible =
            "More than 50% of all in-game objects appear to be inaccessible in this project with your current mod list. This can have a variety of reasons like objects being accessible via scripts,"
        private const val RocketLaunchInaccessible =
            "Rocket launch appear to be inaccessible. This means that rocket may not be launched in this mod pack, or it requires mod script to spawn or unlock some items,"
        private const val MaybeBug = " or it might be due to a bug inside a mod or YAFC."
        private const val MilestoneAnalysisIsImportant =
            "\nA lot of YAFC's systems rely on objects being accessible, so some features may not work as intended."
        private const val UseDependencyExplorer =
            "\n\nFor this reason YAFC has a Dependency Explorer that allows you to manually enable some of the core recipes. YAFC will iteratively try to unlock all the dependencies after each recipe you manually enabled. For most modpacks it's enough to unlock a few early recipes like any special recipes for plates that everything in the mod is based on."
        private const val MilestonesNotReachable =
            "There are some milestones that are not accessible: %s. You may remove these from milestone list,"
    }
}
