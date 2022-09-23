package com.xhlab.yafc.parser

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
import com.twelvemonkeys.io.LittleEndianDataInputStream
import com.xhlab.yafc.model.Version
import com.xhlab.yafc.model.data.DataUtils
import com.xhlab.yafc.model.data.YAFCDatabase
import com.xhlab.yafc.parser.data.deserializer.FactorioDataDeserializer
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import java.io.File
import java.io.FileInputStream
import java.io.Reader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class FactorioDataSource {

    private val allMods = hashMapOf<String, ModInfo?>()
    private val gson = Gson()

    private val logger = Logger.getInstance(FactorioDataSource::class.java)

    var progressListener: ParserProgressChangeListener? = null

    fun resolveModPath(currentMod: String, fullPath: String, isLuaRequire: Boolean = false): Pair<String, String> {
        val splitters = if (isLuaRequire && !fullPath.contains("/")) fileSplittersLua else fileSplittersNormal
        val path = fullPath.split(*splitters)
        if (path.contains("")) {
            throw RuntimeException("Attempt to traverse to parent directory")
        }

        var (mod, resolved) = if (path[0].startsWith("__") && path[0].endsWith("__")) {
            val mod = path[0].substring(2, path[0].length - 2)
            mod to path.drop(1).joinToString("/")
        } else {
            currentMod to path.joinToString("/")
        }

        if (isLuaRequire) {
            resolved += ".lua"
        }

        return mod to resolved
    }

    fun modPathExists(modName: String, path: String): Boolean {
        val info = allMods[modName] ?: return false

        val archive = info.zipArchive
        if (archive != null) {
            return archive.getEntry(info.folder + path) != null
        }

        val file = File(info.folder, path)
        return file.exists()
    }

    internal fun readModFile(modName: String, path: String): ByteArray? {
        val info = allMods[modName] ?: return null

        val folderName = info.folder
        val archive = info.zipArchive
        if (folderName != null && archive != null) {
            val entry = archive.getEntry(folderName + path) ?: return null
            return archive.getInputStream(entry).readBytes()
        }

        val file = File(info.folder, path)
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }

    private fun loadModLocale(modName: String, locale: String) {
        for (localeName in getAllModFiles(modName, "locale/$locale/")) {
            readModFile(modName, localeName)?.inputStream()?.use {
                FactorioLocalization.parse(it.bufferedReader())
            }
        }
    }

    /**
     * find all mods containing info.json regardless of zipped in directory
     */
    private fun findMods(directory: String, mods: MutableList<ModInfo>) {
        val dir = File(directory)
        if (dir.exists() && dir.isDirectory) {
            for (entry in dir.listFiles(File::isDirectory) ?: emptyArray()) {
                val infoFile = File(entry, "info.json")
                if (infoFile.exists()) {
                    sendProgressUpdate("Initializing", entry.name)
                    val info = ModInfo.fromJson(infoFile.reader()).apply {
                        folder = entry.path
                    }
                    mods.add(info)
                }
            }

            for (file in dir.listFiles() ?: emptyArray()) {
                if (file.name.endsWith(".zip", true)) {
                    val zipArchive = ZipFile(file)

                    var infoEntry: ZipEntry? = null
                    for (entry in zipArchive.entries()) {
                        if (entry.name.lowercase().endsWith("info.json")) {
                            infoEntry = entry
                            break
                        }
                    }

                    if (infoEntry != null) {
                        val reader = zipArchive.getInputStream(infoEntry).reader()
                        val info = ModInfo.fromJson(reader).apply {
                            val name = infoEntry.name
                            this.folder = name.substring(0, name.length - "info.json".length)
                            this.zipArchive = zipArchive
                        }
                        mods.add(info)
                    }
                }
            }
        }
    }

    fun parse(
        factorioDataPath: String,
        modPath: String?,
        expensive: Boolean,
        locale: String?,
        yafcVersion: Version,
        renderIcons: Boolean = true
    ): YAFCDatabase {
        try {
            sendCurrentLoadingModChange(null)

            val modSettings = File(modPath, "mod-settings.dat")
            sendProgressUpdate("Initializing", "Loading mod list")

            val modList = File(modPath, "mod-list.json")
            val versionSpecifiers = hashMapOf<String, Version>()

            allMods.clear()
            if (modList.exists()) {
                val mods = gson.fromJson(modList.reader(), ModList::class.java)
                mods.mods.asSequence()
                    .filter { it.enabled }
                    .map { it.name }
                    .forEach { allMods[it] = null }
                mods.mods.asSequence()
                    .filter { it.enabled && !it.version.isNullOrEmpty() }
                    .forEach { versionSpecifiers[it.name] = Version.fromString(it.version!!) }
            } else {
                allMods["base"] = null
            }

            allMods["core"] = null
            logger.debug("Mod list parsed")

            val allFoundMods = arrayListOf<ModInfo>()
            findMods(factorioDataPath, allFoundMods)
            if (modPath != factorioDataPath && !modPath.isNullOrBlank()) {
                findMods(modPath, allFoundMods)
            }

            var factorioVersion: Version? = null
            for (mod in allFoundMods) {
                sendCurrentLoadingModChange(mod.name)
                if (mod.name == "base") {
                    if (factorioVersion == null || mod.parsedVersion > factorioVersion) {
                        factorioVersion = mod.parsedVersion
                    }
                }
            }

            // map allFoundMods to allMods
            for (mod in allFoundMods) {
                sendCurrentLoadingModChange(mod.name)
                val existing = allMods[mod.name]
                val version = versionSpecifiers[mod.name]
                if (mod.validForFactorioVersion(factorioVersion) && allMods.containsKey(mod.name) &&
                    (existing == null || mod.parsedVersion > existing.parsedVersion || (mod.parsedVersion == existing.parsedVersion && existing.zipArchive != null && mod.zipArchive == null)) &&
                    (!versionSpecifiers.containsKey(mod.name) || mod.parsedVersion == version)
                ) {
                    allMods[mod.name] = mod
                }
            }

            for ((name, mod) in allMods.entries) {
                sendCurrentLoadingModChange(name)
                if (mod == null) {
                    throw RuntimeException("Mod not found: $name. Try loading this pack in Factorio first.")
                }
            }

            val modsToDisable = arrayListOf<String>()
            do {
                modsToDisable.clear()
                for ((name, mod) in allMods.entries) {
                    sendCurrentLoadingModChange(name)
                    if (mod != null && !mod.checkDependencies(allMods, modsToDisable)) {
                        modsToDisable.add(name)
                    }
                }

                sendCurrentLoadingModChange(null)

                for (mod in modsToDisable) {
                    allMods.remove(mod)
                }
            } while (modsToDisable.isNotEmpty())

            sendCurrentLoadingModChange(null)
            sendProgressUpdate("Initializing", "Creating Lua context")

            val modsToLoad = allMods.keys.toHashSet()
            val modLoadOrder = Array(modsToLoad.size) { "" }
            modLoadOrder[0] = "core"
            modsToLoad.remove("core")

            var index = 1
            val sortedMods = modsToLoad.sortedBy { it.lowercase() }.toMutableList()
            val currentLoadBatch = arrayListOf<String>()
            while (modsToLoad.size > 0) {
                currentLoadBatch.clear()
                for (mod in sortedMods) {
                    if (allMods[mod]?.canLoad(allMods, modsToLoad) == true) {
                        currentLoadBatch.add(mod)
                    }
                }
                if (currentLoadBatch.isEmpty()) {
                    throw RuntimeException(
                        "Mods dependencies are circular. Unable to load mods: " + modsToLoad.joinToString(", ")
                    )
                }
                for (mod in currentLoadBatch) {
                    modLoadOrder[index++] = mod
                    modsToLoad.remove(mod)
                }

                sortedMods.removeIf { !modsToLoad.contains(it) }
            }

            if (locale != null) {
                for (mod in modLoadOrder) {
                    sendCurrentLoadingModChange(mod)
                    loadModLocale(mod, locale)
                }
            }
            // Fill the rest with the locale keys from english
            if (locale != "en") {
                for (mod in modLoadOrder) {
                    sendCurrentLoadingModChange(mod)
                    loadModLocale(mod, "en")
                }
            }

            logger.info("All mods found! Loading order: " + modLoadOrder.joinToString(", "))

            val preprocess = javaClass.getResourceAsStream("/data/Sandbox.lua")?.readBytes()
                ?: throw RuntimeException("Sandbox.lua not found from resources")
            val postprocess = javaClass.getResourceAsStream("/data/Postprocess.lua")?.readBytes()
                ?: throw RuntimeException("Postprocess.lua not found from resources")

            DataUtils.allMods = modLoadOrder
            DataUtils.dataPath = factorioDataPath
            DataUtils.modsPath = modPath
            DataUtils.expensiveRecipes = expensive

            val dataContext = LuaContext(this, allMods, factorioDataPath, yafcVersion)
            val settings = if (modSettings.exists()) {
                LittleEndianDataInputStream(FileInputStream(modSettings)).use {
                    FactorioPropertyTree().readModSettings(it)
                }.apply {
                    logger.info("Mod settings parsed")
                }
            } else {
                LuaValue.tableOf()
            }

            // TODO default mod settings
            dataContext.setGlobal("settings", settings)

            dataContext.exec(preprocess, "Sandbox.lua")
            dataContext.doModFiles(modLoadOrder, "data.lua")
            dataContext.doModFiles(modLoadOrder, "data-updates.lua")
            dataContext.doModFiles(modLoadOrder, "data-final-fixes.lua")
            dataContext.exec(postprocess, "Postprocess.lua")
            sendCurrentLoadingModChange(null)

            val deserializer = FactorioDataDeserializer(
                dataSource = this,
                data = dataContext.data,
                prototypes = dataContext.defines["prototypes"] as LuaTable,
                renderIcons = renderIcons,
                expensiveRecipes = expensive,
                factorioVersion = factorioVersion ?: defaultFactorioVersion
            )
            val db = deserializer.loadData(progress)
            logger.debug("Completed!")
            sendProgressUpdate("Completed!", "Done creating database")

            return db
        } finally {
            allMods.clear()
        }
    }

    /**
     * get all internal mod files matching prefix
     */
    private fun getAllModFiles(mod: String, prefix: String): List<String> {
        val info = allMods[mod] ?: return emptyList()
        val modFiles = arrayListOf<String>()

        val archive = info.zipArchive
        if (archive != null) {
            for (entry in archive.entries()) {
                val entryLocalPath = entry.name.substringAfter('/')
                if (entryLocalPath.lowercase().startsWith(prefix.lowercase())) {
                    modFiles.add(entryLocalPath)
                }
            }
        } else {
            val dirFrom = File(info.folder, prefix)
            if (dirFrom.exists() && dirFrom.isDirectory) {
                for (file in dirFrom.listFiles() ?: emptyArray()) {
                    modFiles.add(prefix + File.separator + file.name)
                }
            }
        }

        return modFiles
    }

    private fun sendProgressUpdate(title: String, description: String) {
        progressListener?.progressChanged(title, description)
    }

    fun sendCurrentLoadingModChange(mod: String?) {
        progressListener?.currentLoadingModChanged(mod)
    }

    internal data class ModEntry(
        val name: String,
        val enabled: Boolean,
        val version: String?
    )

    internal data class ModList(val mods: ArrayList<ModEntry>)

    data class ModInfo(
        val name: String,
        val version: String?,
        val factorioVersion: String?,
        val dependencies: List<String> = defaultDependencies
    ) {
        val parsedVersion: Version = if (version != null) {
            Version.fromString(version)
        } else {
            Version(0, 0)
        }

        val parsedFactorioVersion: Version = if (factorioVersion != null) {
            Version.fromString(factorioVersion)
        } else {
            defaultFactorioVersion
        }

        private val _incompatibilities = arrayListOf<String>()
        val incompatibilities: List<String> = _incompatibilities
        val parsedDependencies: List<Dependency> = parseDependencies()

        var zipArchive: ZipFile? = null
        var folder: String? = null

        private fun parseDependencies(): List<Dependency> {
            val dependencyList = arrayListOf<Dependency>()
            for (dependency in dependencies) {
                val match = dependencyRegex.matchEntire(dependency)
                if (match != null && !match.range.isEmpty()) {
                    val modifier = match.groupValues[1]
                    if (modifier == "!") {
                        _incompatibilities.add(match.groupValues[2])
                        continue
                    }
                    if (modifier == "~") {
                        continue
                    }

                    dependencyList.add(Dependency(match.groupValues[2], modifier == "?"))
                }
            }

            return dependencyList
        }

        private fun majorMinorEquals(a: Version, b: Version): Boolean {
            return (a.major == b.major && a.minor == b.minor)
        }

        fun validForFactorioVersion(factorioVersion: Version?): Boolean {
            return ((factorioVersion == null || majorMinorEquals(factorioVersion, parsedFactorioVersion)) ||
                    (majorMinorEquals(factorioVersion, Version(1, 0)) &&
                            majorMinorEquals(parsedFactorioVersion, Version(0, 18))) ||
                    name == "core")
        }

        fun checkDependencies(allMods: Map<String, ModInfo?>, modsToDisable: List<String>): Boolean {
            for ((mod, optional) in parsedDependencies) {
                if (!optional && !allMods.containsKey(mod)) {
                    return false
                }
            }

            for (incompatible in incompatibilities) {
                if (allMods.containsKey(incompatible) && !modsToDisable.contains(incompatible)) {
                    return false
                }
            }

            return true
        }

        fun canLoad(mods: Map<String, ModInfo?>, nonLoadedMods: Set<String>): Boolean {
            for ((mod, _) in parsedDependencies) {
                if (nonLoadedMods.contains(mod)) {
                    return false
                }
            }

            return true
        }

        data class Dependency(val mod: String, val optional: Boolean)

        companion object {
            private val gson by lazy { Gson() }

            fun fromJson(reader: Reader): ModInfo = with(gson.fromJson(reader, JsonObject::class.java)) {
                return ModInfo(
                    name = get("name").asString,
                    version = get("version")?.asString,
                    factorioVersion = get("factorio_version")?.asString,
                    dependencies = getAsJsonArray("dependencies")?.map { it.asString } ?: emptyList()
                )
            }
        }
    }

    companion object {
        val defaultFactorioVersion = Version(1, 1)

        private val fileSplittersLua = charArrayOf('.', '/', '\\')
        private val fileSplittersNormal = charArrayOf('/', '\\')

        private val defaultDependencies = listOf("base")
        private val dependencyRegex = Regex("^\\(?([?!~]?)\\)?\\s*([\\w- ]+?)(?:\\s*[><=]+\\s*[\\d.]*)?\\s*$")
    }
}
