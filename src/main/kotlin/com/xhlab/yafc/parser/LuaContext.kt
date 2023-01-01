package com.xhlab.yafc.parser

import com.xhlab.yafc.model.Version
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

class LuaContext constructor(
    private val dataSource: FactorioDataSource,
    allMods: Map<String, FactorioDataSource.ModInfo?>,
    factorioDataPath: String,
    yafcVersion: Version,
    private val logger: YAFCLogger
) {
    private val modFixes = hashMapOf<ModNamePair, ByteArray>()
    private val loadingMod = ArrayDeque<String>()

    private val globals = JsePlatform.debugGlobals()
    private val packages = globals["package"] as LuaTable
    private val oldRequire: LuaFunction

    val data: LuaTable
        get() = (globals["data"] as? LuaTable) ?: LuaTable.tableOf()

    val defines: LuaTable
        get() = (globals["defines"] as? LuaTable) ?: LuaTable.tableOf()

    init {
        registerApi("raw_log", Log())

        registerApi("set_lua_path", SetLuaPath())

        packages.set("loading", LuaValue.tableOf())
        packages.set("loaded", LuaValue.tableOf())

        oldRequire = globals["require"] as LuaFunction
        setGlobal("require", Require())

        setGlobal("yafc_version", LuaValue.valueOf(yafcVersion.toString()))

        val mods = LuaValue.tableOf()
        for (mod in allMods) {
            mods[mod.key] = mod.value?.version?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        }
        setGlobal("mods", mods)

        val uri = javaClass.getResource("/data/mod-fixes/")?.toURI()
        if (uri != null) {
            val fixes = File(uri.path)
            if (fixes.exists() && fixes.isDirectory) {
                val luaFiles = (fixes.listFiles() ?: emptyArray()).filter { it.extension == "lua" }
                for (file in luaFiles) {
                    val fileName = file.name
                    val modAndFile = fileName.split('.')
                    val assemble = modAndFile.drop(1).dropLast(1).joinToString("/")
                    modFixes[ModNamePair(modAndFile[0], "$assemble.lua")] = file.readBytes()
                }
            }
        }

        // add resources
        setLuaPath("/data/?.lua")

        // load core lualib
        setLuaPath("$factorioDataPath/core/lualib/?.lua")

        logger.info<LuaContext>(globals.load("print(package.path)").call().tojstring())
    }

    fun setGlobal(name: String, value: LuaValue) {
        globals.set(name, value)
    }

    inner class Log : OneArgFunction() {

        override fun call(arg: LuaValue?): LuaValue {
            val log = arg?.tojstring()
            if (log != null) {
                logger.info<LuaContext>(log)
            }

            return valueOf(0)
        }
    }

    inner class SetLuaPath : OneArgFunction() {

        override fun call(arg: LuaValue?): LuaValue {
            val luaPath = arg?.tojstring() ?: throw NullPointerException("Lua path not provided")
            val prevPath = packages["path"]
            val path = if (prevPath != LuaValue.NIL) {
                "${prevPath.tojstring()};$luaPath"
            } else {
                luaPath
            }
            packages["path"] = path

            return LuaValue.NONE
        }
    }

    inner class Require : OneArgFunction() {

        override fun call(arg: LuaValue?): LuaValue {
            var file = arg?.tojstring() ?: throw NullPointerException("Require target is empty")

            if (file.contains("..")) {
                throw RuntimeException("Attempt to traverse to parent directory")
            }
            if (file.lowercase().endsWith(".lua")) {
                file = file.substring(0, file.length - 4)
            }

            file = file.replace('\\', '/')
            val origFile = file
            file = file.replace('.', '/')
            val fileExt = "$file.lua"

            val loading = packages["loading"] as LuaTable
            if (loading[fileExt]?.toboolean() == true) {
                return LuaValue.NONE
            }

            val loadedFile = (packages["loaded"] as LuaTable)[fileExt]
            if (loadedFile != null && loadedFile != LuaValue.NIL) {
                return loadedFile
            }

            loading[fileExt] = LuaValue.valueOf(true)

            val loadingModName = loadingMod.lastOrNull()

            val result = if (loadingModName == null) {
                requireGlobal(origFile)
            } else {
                val modName = fileExt.substringBefore("/")
                val (actualModName, fileName) = if (modName.startsWith("__") && modName.endsWith("__")) {
                    val actualModName = modName.substring(2, modName.length - 2)
                    val fileName = fileExt.substringAfter("/")
                    actualModName to fileName
                } else {
                    loadingModName to fileExt
                }

                val modFileExt = "$actualModName:$fileExt"
                val loadedModFile = (packages["loaded"] as LuaTable)[modFileExt]
                if (loadedModFile != null && loadedModFile != LuaValue.NIL) {
                    logger.info<LuaContext>("Loaded mod local module : $actualModName : $file")
                    loadedModFile
                } else {
                    // TODO: add mod-fix support
                    val module = doModFile(actualModName, fileName)
                    if (module != LuaValue.NIL) {
                        (packages["loaded"] as LuaTable)[modFileExt] = module
                        logger.info<LuaContext>("Loaded local module : $file")
                        module
                    } else if (!fileExt.contains('/')) {
                        requireGlobal(origFile)
                    } else {
                        LuaValue.NIL
                    }
                }
            }

            loading[fileExt] = LuaValue.valueOf(false)

            return result
        }

        private fun requireGlobal(file: String): LuaValue {
            return try {
                val result = oldRequire.call(file)
                logger.info<LuaContext>("Loaded global module : $file")
                result
            } catch (e: LuaError) {
                logger.info<LuaContext>("Error while requiring file $file : ${e.message}")
                LuaValue.NIL
            }
        }
    }

    private fun registerApi(name: String, callback: LuaFunction) {
        globals.set(name, callback)
    }

    fun exec(bytes: ByteArray, chunkName: String) {
        globals.load(bytes.toString(Charsets.UTF_8), chunkName).call()
    }

    fun doModFiles(modOrder: Array<String>, fileName: String) {
        for (mod in modOrder) {
            loadingMod.addLast(mod)
            dataSource.sendCurrentLoadingModChange(mod)
            doModFile(mod, fileName)
            loadingMod.removeLast()
        }
    }

    private fun doModFile(mod: String, fileName: String): LuaValue {
        try {
            val bytes = dataSource.readModFile(mod, fileName)
            if (bytes != null) {
                if (bytes.isEmpty()) {
                    logger.info<LuaContext>("Zero byte : $mod/$fileName")
                    return LuaValue.NIL
                }

                logger.info<LuaContext>("⬜ Executing $mod/$fileName : ${bytes.size} bytes")

                val string = bytes.toString(Charsets.UTF_8)

                return globals.load(string, fileName).call()
            } else {
                logger.info<LuaContext>("Could not found mod file : $mod/$fileName")
            }
        } catch (e: LuaError) {
            logger.info<LuaContext>("Lua error while executing $mod/$fileName : ${e.message}")
        }

        return LuaValue.NIL
    }

    private fun setLuaPath(path: String) {
        globals.load("set_lua_path('$path')").call()
    }

    data class ModNamePair(val mod: String, val name: String)
}
