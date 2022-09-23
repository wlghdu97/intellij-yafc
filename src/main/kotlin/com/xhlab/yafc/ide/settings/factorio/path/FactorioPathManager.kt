package com.xhlab.yafc.ide.settings.factorio.path

import com.dd.plist.NSDictionary
import com.dd.plist.XMLPropertyListParser
import com.google.common.collect.ImmutableList
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.text.SemVer
import com.xhlab.yafc.ide.settings.factorio.TimestampedSemver
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPathChangeListener.Companion.YAFC_FACTORIO_PATH_TOPIC
import com.xhlab.yafc.ide.settings.factorio.path.FactorioPathRef.Companion.PROJECT_FACTORIO_PATH_REF
import org.jdom.Element
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@State(
    name = "FactorioPathManager",
    storages = [Storage("yafc.xml")]
)
class FactorioPathManager : PersistentStateComponent<Element> {
    @Volatile
    private var internalFactorioPaths: List<FactorioPath>? = null

    @Volatile
    private var factorioPathByRefNameMap: Map<String, FactorioPath> = emptyMap()

    @Volatile
    private var projectPathRef: FactorioPathRef? = null
    private val cachedPathMapByName = ConcurrentHashMap<String, FactorioPath>()
    private val versionCache = ConcurrentHashMap<String, TimestampedSemver>()

    fun getCachedVersion(path: FactorioPath): Ref<SemVer>? {
        val independentPath = path.systemIndependentPath
        val tsv = versionCache[independentPath]
        if (tsv != null) {
            val file = File(independentPath)
            if (file.lastModified() == tsv.lastModified) {
                if (tsv.version == null && tsv.loadedFromXml) {
                    fetchVersion(path)
                }

                return Ref.create(tsv.version)
            }

            versionCache.remove(independentPath)
        }

        return null
    }

    fun fetchVersion(path: FactorioPath, afterFetch: (SemVer?) -> Unit = { }) {
        val semVer = if (SystemInfo.isMac) {
            val infoPlist = File(path.file, "Contents/Info.plist")
            if (infoPlist.isFile) {
                try {
                    val info = XMLPropertyListParser.parse(infoPlist) as NSDictionary
                    val version = info.objectForKey("CFBundleGetInfoString").toString()
                    SemVer.parseFromText(version)
                } catch (e: Throwable) {
                    logger.debug("failed to fetch version : ${path.presentableName}")
                    null
                }
            } else {
                null
            }
        } else {
            null
        }

        val tsv = TimestampedSemver(semVer, path.file.lastModified(), false)
        versionCache[path.systemIndependentPath] = tsv
        afterFetch.invoke(semVer)
    }

    private fun getDetectedFactorioPaths(): List<String> {
        return FactorioPathUtil.detectAllFactorioPaths().map { FileUtil.toSystemIndependentName(it.absolutePath) }
    }

    fun getProjectFactorioPathRef(project: Project): FactorioPathRef {
        val internalProjectPathRef = projectPathRef
        return if (internalProjectPathRef == null) {
            val propertiesComponent = PropertiesComponent.getInstance(project)
            val reference = propertiesComponent.getValue(FACTORIO_PATH)
            val newRef = if (!reference.isNullOrBlank() && reference != PROJECT_FACTORIO_PATH_REF) {
                FactorioPathRef.create(reference)
            } else {
                FactorioPathRef.create("")
            }
            projectPathRef = newRef
            newRef
        } else {
            internalProjectPathRef
        }
    }

    fun setProjectFactorioPath(project: Project, pathRef: FactorioPathRef?) {
        val oldFactorioPathRef = projectPathRef
        projectPathRef = pathRef
        val propertiesComponent = PropertiesComponent.getInstance(project)
        if (pathRef == null) {
            propertiesComponent.unsetValue(FACTORIO_PATH)
        } else {
            if (pathRef.isProjectRef()) {
                throw IllegalArgumentException("Project factorio path cannot be set to itself")
            }

            propertiesComponent.setValue(FACTORIO_PATH, pathRef.referenceName)
        }

        fireFactorioPathChangeIfNeeded(project, oldFactorioPathRef, pathRef)
    }

    private fun fireFactorioPathChangeIfNeeded(
        project: Project,
        oldPathRef: FactorioPathRef?,
        newPathRef: FactorioPathRef?
    ) {
        val oldPath = oldPathRef?.resolve(project)
        val newPath = newPathRef?.resolve(project)
        if (oldPath != newPath) {
            project.messageBus.syncPublisher(YAFC_FACTORIO_PATH_TOPIC).factorioPathChanged(newPath)
        }
    }

    fun setFactorioPaths(paths: List<FactorioPath>) {
        internalFactorioPaths = ImmutableList.copyOf(paths)
        factorioPathByRefNameMap = ContainerUtil.newMapFromValues(paths.iterator()) { it.presentableName }
    }

    fun getFactorioPaths(): List<FactorioPath> {
        val detectedPaths = HashSet(getDetectedFactorioPaths())
        val result = ArrayList(ContainerUtil.notNullize(internalFactorioPaths))
        result.forEach {
            detectedPaths.remove(it.systemIndependentPath)
        }
        detectedPaths.forEach {
            result.add(FactorioPath(it))
        }
        setFactorioPaths(result)

        return result
    }

    fun resolveReference(referenceName: String): FactorioPath? {
        return if (referenceName.isBlank() || !OSAgnosticPathUtil.isAbsolute(referenceName)) {
            null
        } else {
            findByReferenceNameOrCreate(referenceName)
        }
    }

    private fun resolveFactorioPath(referenceName: String): String {
        return if (SystemInfo.isWindows) {
            val fileWithExePath = "$referenceName.exe"
            val fileWithExe = File(fileWithExePath)
            if (fileWithExe.isFile) {
                fileWithExePath
            } else {
                val file = File(referenceName)
                if (file.isFile) {
                    referenceName
                } else {
                    fileWithExePath
                }
            }
        } else {
            referenceName
        }
    }

    private fun findByReferenceNameOrCreate(referenceName: String): FactorioPath {
        val path = factorioPathByRefNameMap[referenceName]
        return if (path == null) {
            val cachedPath = cachedPathMapByName[referenceName]
            if (cachedPath == null) {
                val resolvedPath = resolveFactorioPath(referenceName)
                val resolvedFactorioPath = FactorioPath(resolvedPath)
                cachedPathMapByName.putIfAbsent(referenceName, resolvedFactorioPath)
                resolvedFactorioPath
            } else {
                cachedPath
            }
        } else {
            path
        }
    }

    override fun getState(): Element? {
        val internalPaths = internalFactorioPaths
        return if (internalPaths == null) {
            null
        } else {
            Element(FACTORIO_PATHS_TAG_NAME).apply {
                internalPaths.forEach {
                    val pathElement = Element(FACTORIO_PATH_TAG_NAME).apply {
                        setAttribute(PATH_ATTR_NAME, it.systemIndependentPath)
                    }
                    addContent(pathElement)
                }
            }
        }
    }

    override fun loadState(state: Element) {
        val children = state.getChildren(FACTORIO_PATH_TAG_NAME)
        val factorioPaths = arrayListOf<FactorioPath>()

        children.forEach {
            val path = it.getAttributeValue(PATH_ATTR_NAME)
            if (path != null) {
                val independentPath = FileUtil.toSystemIndependentName(path)
                factorioPaths.add(FactorioPath(independentPath))
            }
        }

        setFactorioPaths(factorioPaths)
    }

    companion object {
        private val logger = Logger.getInstance(FactorioPathManager::class.java)

        private const val FACTORIO_PATHS_TAG_NAME = "factorio_paths"
        private const val FACTORIO_PATH_TAG_NAME = "factorio_path"
        private const val PATH_ATTR_NAME = "path"
        private const val FACTORIO_PATH = "factorio_path"
    }
}
