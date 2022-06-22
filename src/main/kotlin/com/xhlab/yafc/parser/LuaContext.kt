package com.xhlab.yafc.parser

import com.intellij.openapi.diagnostic.Logger
import com.xhlab.yafc.model.Project
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
    factorioPath: String
) {
    private val modFixes = hashMapOf<ModNamePair, ByteArray>()
    private val loadingMod = ArrayDeque<String>()

    private val globals = JsePlatform.debugGlobals()
    private val packages = globals["package"] as LuaTable
    private val oldRequire: LuaFunction

    private val logger = Logger.getInstance(LuaContext::class.java)

    init {
        registerApi("raw_log", Log())

        registerApi("set_lua_path", SetLuaPath())

        packages.set("loading", LuaValue.tableOf())
        packages.set("loaded", LuaValue.tableOf())

        oldRequire = globals["require"] as LuaFunction
        setGlobal("require", Require())

        setGlobal("yafc_version", LuaValue.valueOf(Project.currentYafcVersion.toString()))

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
        setLuaPath("$factorioPath/core/lualib/?.lua")

        logger.info(globals.load("print(package.path)").call().tojstring())
    }

    fun getGlobal(name: String): LuaValue? {
        return globals.get(name)
    }

    fun setGlobal(name: String, value: LuaValue) {
        globals.set(name, value)
    }

    inner class Log : OneArgFunction() {

        override fun call(arg: LuaValue?): LuaValue {
            val log = arg?.tojstring()
            if (log != null) {
                logger.info(log)
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
            if (loading[origFile]?.toboolean() == true) {
                return LuaValue.NONE
            }

            val loadedFile = (packages["loaded"] as LuaTable)[origFile]
            if (loadedFile != null && loadedFile != LuaValue.NIL) {
                return loadedFile
            }

            loading[origFile] = LuaValue.valueOf(true)

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

                // TODO: add mod-fix support
                val module = doModFile(actualModName, fileName)
                if (module != LuaValue.NIL) {
                    (packages["loaded"] as LuaTable)[origFile] = module
                    logger.info("Loaded local module : $file")
                    module
                } else if (fileExt.split('/').isEmpty()) {
                    requireGlobal(origFile)
                } else {
                    LuaValue.NIL
                }
            }

            loading[origFile] = LuaValue.valueOf(false)

            return result
        }

        private fun requireGlobal(file: String): LuaValue {
            return try {
                val result = oldRequire.call(file)
                logger.info("Loaded global module : $file")
                result
            } catch (e: LuaError) {
                logger.info("Error while requiring file $file : ${e.message}")
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
                    logger.info("Zero byte : $mod/$fileName")
                    return LuaValue.NIL
                }

                logger.info("⬜ Executing $mod/$fileName : ${bytes.size} bytes")

                val string = bytes.toString(Charsets.UTF_8)

                return globals.load(string, fileName).call()
            } else {
                logger.info("Could not found mod file : $mod/$fileName")
            }
        } catch (e: LuaError) {
            logger.info("Lua error while executing $mod/$fileName : ${e.message}")
        }

        return LuaValue.NIL
    }

    private fun setLuaPath(path: String) {
        globals.load("set_lua_path('$path')").call()
    }

    data class ModNamePair(val mod: String, val name: String)
}