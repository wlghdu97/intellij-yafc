package com.xhlab.yafc.ide.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.LayeredIcon
import com.intellij.util.IconUtil
import com.intellij.util.ui.ImageUtil
import com.xhlab.yafc.ide.ui.IconCollection.IconSize.Companion.actualSize
import com.xhlab.yafc.model.data.FactorioIconPart
import com.xhlab.yafc.model.data.FactorioObject
import com.xhlab.yafc.model.data.Recipe
import com.xhlab.yafc.parser.FactorioDataSource
import java.awt.Color
import java.awt.Image
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.awt.image.RGBImageFilter
import javax.swing.Icon
import kotlin.math.min
import kotlin.math.roundToInt

object IconCollection {
    const val ICON_SIZE = 16
    const val BIG_ICON_SIZE = ICON_SIZE * 2

    private val iconCache = hashMapOf<IconCacheKey, Icon?>()

    fun resetIconCache() {
        iconCache.clear()
    }

    fun getSmallIcon(
        dataSource: FactorioDataSource,
        obj: FactorioObject,
        size: IconSize = IconSize.NORMAL,
        gravity: IconGravity = IconGravity.CENTER
    ): Icon? {
        return getIcon(dataSource, obj, size, IconZoom.HALF, gravity)
    }

    fun getIcon(dataSource: FactorioDataSource, obj: FactorioObject): Icon? {
        return getIcon(dataSource, obj, IconSize.NORMAL)
    }

    fun getBigIcon(dataSource: FactorioDataSource, obj: FactorioObject): Icon? {
        return getIcon(dataSource, obj, IconSize.BIG)
    }

    private fun getIcon(
        dataSource: FactorioDataSource,
        obj: FactorioObject,
        size: IconSize,
        zoom: IconZoom = IconZoom.SAME,
        gravity: IconGravity = IconGravity.CENTER
    ): Icon? {
        val iconSpec = obj.iconSpec
        if (iconSpec.isNotEmpty()) {
            val cacheKey = IconCacheKey(iconSpec[0], size, zoom, gravity)
            val simpleSprite = (iconSpec.size == 1 && iconSpec[0].isSimple())
            val cachedIcon = iconCache[cacheKey]
            if (simpleSprite && cachedIcon != null) {
                return cachedIcon
            }

            return try {
                val icon = createIconFromSpec(dataSource, size, zoom, gravity, *iconSpec.toTypedArray())
                if (simpleSprite) {
                    iconCache[cacheKey] = icon
                }
                icon
            } catch (e: Exception) {
                null
            }
        } else if (obj is Recipe) {
            val mainProduct = obj.mainProduct
            if (mainProduct != null) {
                return createIconFromSpec(dataSource, size, zoom, gravity, *mainProduct.iconSpec.toTypedArray())
            }
        }

        return null
    }

    private fun createIconFromSpec(
        dataSource: FactorioDataSource,
        size: IconSize,
        zoom: IconZoom,
        gravity: IconGravity,
        vararg spec: FactorioIconPart
    ): Icon? {
        val images = arrayListOf<Icon>()
        val applyScale = (spec.size > 1)
        for (icon in spec) {
            val cacheKey = IconCacheKey(icon, size, zoom, gravity)
            val (mod, path) = ApplicationManager.getApplication().runReadAction<Pair<String, String>> {
                dataSource.resolveModPath("", icon.path)
            }
            var image = iconCache[cacheKey]
            if (image == null) {
                val imageSource = ApplicationManager.getApplication().runReadAction<ByteArray?> {
                    dataSource.readModFile(mod, path)
                }
                if (imageSource == null) {
                    iconCache[cacheKey] = null
                } else {
                    val rawImage = Toolkit.getDefaultToolkit().createImage(imageSource)
                    if (rawImage != null) {
                        image = rawImage.createIconWithSpec(icon, applyScale, size, zoom, gravity)
                    }
                    iconCache[cacheKey] = image
                }
            }
            if (image != null) {
                images.add(image)
            }
        }

        val resizedImages = images.map { IconUtil.resizeSquared(it, size.actualSize) }
        return when (resizedImages.size) {
            0 -> {
                null
            }

            1 -> {
                resizedImages[0]
            }

            else -> {
                LayeredIcon(*resizedImages.toTypedArray()).withIconPreScaled(true)
            }
        }
    }

    private fun Image.createIconWithSpec(
        spec: FactorioIconPart,
        applyScale: Boolean,
        type: IconSize,
        zoom: IconZoom = IconZoom.SAME,
        gravity: IconGravity = IconGravity.CENTER
    ): Icon {
        val tempIcon = IconUtil.createImageIcon(this)
        val size = min(tempIcon.iconWidth, tempIcon.iconHeight)
        val iconSize = type.actualSize
        val scaledImage = ImageUtil.createImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                val targetSize = if (applyScale) {
                    (iconSize * spec.scale).toInt()
                } else {
                    iconSize
                }
                val iconShiftUnit = iconSize / 32f
                val targetRect = if (spec.x != 0f || spec.y != 0f) {
                    val targetWidth = (iconShiftUnit * (spec.size * spec.scale)).toInt()
                    val basePosition = (iconSize - targetWidth) / 2f
                    val x = (basePosition + (spec.x * iconShiftUnit)).roundToInt()
                    val y = (basePosition + (spec.y * iconShiftUnit)).roundToInt()
                    Rectangle(x, y, targetWidth, targetWidth)
                } else {
                    val basePosition = ((iconSize - targetSize) / 2f).roundToInt()
                    Rectangle(basePosition, basePosition, targetSize, targetSize)
                }

                val scaledRect = when (zoom) {
                    IconZoom.HALF -> {
                        when (gravity) {
                            IconGravity.CENTER -> {
                                val x = targetRect.x + ((targetRect.centerX - targetRect.x) / 2).roundToInt()
                                val y = targetRect.y + ((targetRect.centerY - targetRect.y) / 2).roundToInt()
                                Rectangle(x, y, targetRect.width / 2, targetRect.height / 2)
                            }

                            IconGravity.RIGHT_BOTTOM -> {
                                val x = targetRect.centerX.toInt()
                                val y = targetRect.centerY.toInt()
                                Rectangle(x, y, targetRect.width / 2, targetRect.height / 2)
                            }
                        }
                    }

                    else -> {
                        targetRect
                    }
                }

                drawImage(
                    this@createIconWithSpec,
                    scaledRect.x,
                    scaledRect.y,
                    scaledRect.x + scaledRect.width,
                    scaledRect.y + scaledRect.height,
                    0,
                    0,
                    size,
                    size,
                    null
                )

                dispose()
            }
        }

        val filteredImage = if (spec.r != 1.0f || spec.g != 1.0f || spec.b != 1.0f) {
            ImageUtil.filter(scaledImage, object : RGBImageFilter() {
                override fun filterRGB(x: Int, y: Int, rgb: Int): Int {
                    val originalColor = Color(rgb, true)
                    val filteredColor = ColorUtil.toAlpha(
                        Color(
                            (originalColor.red * spec.r).roundToInt(),
                            (originalColor.green * spec.g).roundToInt(),
                            (originalColor.blue * spec.b).roundToInt()
                        ),
                        originalColor.alpha
                    )
                    return filteredColor.rgb
                }
            })
        } else {
            scaledImage
        }

        return IconUtil.createImageIcon(filteredImage)
    }

    data class IconCacheKey(
        val spec: FactorioIconPart,
        val size: IconSize,
        val zoom: IconZoom = IconZoom.SAME,
        val gravity: IconGravity = IconGravity.CENTER
    )

    enum class IconSize {
        NORMAL, BIG;

        companion object {
            val IconSize.actualSize: Int
                get() = when (this) {
                    NORMAL -> ICON_SIZE
                    BIG -> BIG_ICON_SIZE
                }
        }
    }

    enum class IconZoom {
        HALF, SAME;
    }

    enum class IconGravity {
        CENTER, RIGHT_BOTTOM;
    }
}
