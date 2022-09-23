package com.xhlab.yafc.ide.settings.factorio.modpath

import com.google.common.collect.ImmutableList
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.util.containers.ContainerUtil
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathChangeListener.Companion.YAFC_FACTORIO_MOD_PATH_TOPIC
import com.xhlab.yafc.ide.settings.factorio.modpath.FactorioModPathRef.Companion.PROJECT_FACTORIO_MOD_PATH_REF
import org.jdom.Element
import java.util.concurrent.ConcurrentHashMap

@State(
    name = "FactorioModPathManager",
    storages = [Storage("yafc.xml")]
)
class FactorioModPathManager : PersistentStateComponent<Element> {
    @Volatile
    private var internalFactorioModPaths: List<FactorioModPath>? = null

    @Volatile
    private var factorioModPathByRefNameMap: Map<String, FactorioModPath> = emptyMap()

    @Volatile
    private var projectModPathRef: FactorioModPathRef? = null
    private val cachedModPathMapByName = ConcurrentHashMap<String, FactorioModPath>()

    private fun getDetectedFactorioModPaths(): List<String> {
        return FactorioModPathUtil.detectAllFactorioModPaths().map { FileUtil.toSystemIndependentName(it.absolutePath) }
    }

    fun getProjectFactorioModPathRef(project: Project): FactorioModPathRef {
        val internalProjectModPathRef = projectModPathRef
        return if (internalProjectModPathRef == null) {
            val propertiesComponent = PropertiesComponent.getInstance(project)
            val reference = propertiesComponent.getValue(FACTORIO_MOD_PATH)
            val newRef = if (!reference.isNullOrBlank() && reference != PROJECT_FACTORIO_MOD_PATH_REF) {
                FactorioModPathRef.create(reference)
            } else {
                FactorioModPathRef.create("")
            }
            projectModPathRef = newRef
            newRef
        } else {
            internalProjectModPathRef
        }
    }

    fun setProjectFactorioModPath(project: Project, modPathRef: FactorioModPathRef?) {
        val oldFactorioModPathRef = projectModPathRef
        projectModPathRef = modPathRef
        val propertiesComponent = PropertiesComponent.getInstance(project)
        if (modPathRef == null) {
            propertiesComponent.unsetValue(FACTORIO_MOD_PATH)
        } else {
            if (modPathRef.isProjectRef()) {
                throw IllegalArgumentException("Project factorio mod path cannot be set to itself")
            }

            propertiesComponent.setValue(FACTORIO_MOD_PATH, modPathRef.referenceName)
        }

        fireFactorioModPathChangeIfNeeded(project, oldFactorioModPathRef, modPathRef)
    }

    private fun fireFactorioModPathChangeIfNeeded(
        project: Project,
        oldModPathRef: FactorioModPathRef?,
        newModPathRef: FactorioModPathRef?
    ) {
        val oldModPath = oldModPathRef?.resolve(project)
        val newModPath = newModPathRef?.resolve(project)
        if (oldModPath != newModPath) {
            project.messageBus.syncPublisher(YAFC_FACTORIO_MOD_PATH_TOPIC).factorioModPathChanged(newModPath)
        }
    }

    fun setFactorioModPaths(paths: List<FactorioModPath>) {
        internalFactorioModPaths = ImmutableList.copyOf(paths)
        factorioModPathByRefNameMap = ContainerUtil.newMapFromValues(paths.iterator()) { it.presentableName }
    }

    fun getFactorioModPaths(): List<FactorioModPath> {
        val detectedPaths = HashSet(getDetectedFactorioModPaths())
        val result = ArrayList(ContainerUtil.notNullize(internalFactorioModPaths))
        result.forEach {
            detectedPaths.remove(it.systemIndependentPath)
        }
        detectedPaths.forEach {
            result.add(FactorioModPath(it))
        }
        setFactorioModPaths(result)

        return result
    }

    fun resolveReference(referenceName: String): FactorioModPath? {
        return if (referenceName.isBlank() || !OSAgnosticPathUtil.isAbsolute(referenceName)) {
            null
        } else {
            findByReferenceNameOrCreate(referenceName)
        }
    }

    private fun findByReferenceNameOrCreate(referenceName: String): FactorioModPath {
        val modPath = factorioModPathByRefNameMap[referenceName]
        return if (modPath == null) {
            val cachedModPath = cachedModPathMapByName[referenceName]
            if (cachedModPath == null) {
                val factorioModPath = FactorioModPath(referenceName)
                cachedModPathMapByName.putIfAbsent(referenceName, factorioModPath)
                factorioModPath
            } else {
                cachedModPath
            }
        } else {
            modPath
        }
    }

    override fun getState(): Element? {
        val internalModPaths = internalFactorioModPaths
        return if (internalModPaths == null) {
            null
        } else {
            Element(FACTORIO_MOD_PATHS_TAG_NAME).apply {
                internalModPaths.forEach {
                    val pathElement = Element(FACTORIO_MOD_PATH_TAG_NAME).apply {
                        setAttribute(PATH_ATTR_NAME, it.systemIndependentPath)
                    }
                    addContent(pathElement)
                }
            }
        }
    }

    override fun loadState(state: Element) {
        val children = state.getChildren(FACTORIO_MOD_PATH_TAG_NAME)
        val factorioModPaths = arrayListOf<FactorioModPath>()

        children.forEach {
            val path = it.getAttributeValue(PATH_ATTR_NAME)
            if (path != null) {
                val independentPath = FileUtil.toSystemIndependentName(path)
                factorioModPaths.add(FactorioModPath(independentPath))
            }
        }

        setFactorioModPaths(factorioModPaths)
    }

    companion object {
        private const val FACTORIO_MOD_PATHS_TAG_NAME = "factorio_mod_paths"
        private const val FACTORIO_MOD_PATH_TAG_NAME = "factorio_mod_path"
        private const val PATH_ATTR_NAME = "path"
        private const val FACTORIO_MOD_PATH = "factorio_mod_path"
    }
}
