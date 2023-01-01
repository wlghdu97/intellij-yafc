package com.xhlab.yafc.ide.project

import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator
import com.sun.jna.Platform
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class YAFCPreloadActivity : PreloadingActivity() {

    override fun preload(indicator: ProgressIndicator) {
        IntellijORToolsLoader.loadNativeLibraries()
    }

    /**
     * @see com.google.ortools.Loader
     */
    object IntellijORToolsLoader {
        private val resourcePath = "ortools-${Platform.RESOURCE_PREFIX}"
        private var loaded = false

        private val nativeResourceURI: URI
            get() {
                val loader = javaClass.classLoader
                val resourceURL = loader.getResource(resourcePath)
                requireNotNull(resourceURL) {
                    String.format("Resource %s was not found in ClassLoader %s", resourcePath)
                }
                return try {
                    resourceURL.toURI()
                } catch (e: URISyntaxException) {
                    throw IOException(e)
                }
            }

        private fun unpackNativeResources(resourceURI: URI): Path {
            val tempPath = Files.createTempDirectory("ortools-java")
            tempPath.toFile().deleteOnExit()
            val visitor = object : PathConsumer {
                override fun accept(sourcePath: Path) {
                    Files.walkFileTree(sourcePath, object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            val newPath = tempPath.resolve(sourcePath.parent.relativize(file).toString())
                            Files.copy(file, newPath)
                            newPath.toFile().deleteOnExit()
                            return FileVisitResult.CONTINUE
                        }

                        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                            val newPath = tempPath.resolve(sourcePath.parent.relativize(dir).toString())
                            Files.copy(dir, newPath)
                            newPath.toFile().deleteOnExit()
                            return FileVisitResult.CONTINUE
                        }
                    })
                }
            }

            val fs = try {
                FileSystems.newFileSystem(resourceURI, emptyMap<String, Any>())
            } catch (e: FileSystemAlreadyExistsException) {
                FileSystems.getFileSystem(resourceURI).apply {
                    requireNotNull(this)
                }
            }

            val p = fs.provider().getPath(resourceURI)
            visitor.accept(p)
            return tempPath
        }

        @Synchronized
        fun loadNativeLibraries() {
            if (!loaded) {
                try {
                    val tempPath = unpackNativeResources(nativeResourceURI)
                    System.load(tempPath.resolve(resourcePath).resolve(System.mapLibraryName("jniortools")).toString())
                    loaded = true
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }

        private interface PathConsumer {
            fun accept(sourcePath: Path)
        }
    }
}
