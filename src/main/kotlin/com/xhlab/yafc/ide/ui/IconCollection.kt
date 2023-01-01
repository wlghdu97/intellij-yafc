package com.xhlab.yafc.ide.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.LayeredIcon
import com.intellij.util.IconUtil
import com.intellij.util.ui.ImageUtil
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
    private const val ICON_SIZE = 16
    private const val ICON_SHIFT_UNIT = ICON_SIZE / 32f

    private val smallIconCache = hashMapOf<FactorioIconPart, Icon?>()
    private val normalIconCache = hashMapOf<FactorioIconPart, Icon?>()

    fun resetIconCache() {
        smallIconCache.clear()
        normalIconCache.clear()
    }

    fun getSmallIcon(dataSource: FactorioDataSource, obj: FactorioObject): Icon? {
        return getIcon(dataSource, obj, IconType.SMALL)
    }

    fun getIcon(dataSource: FactorioDataSource, obj: FactorioObject): Icon? {
        return getIcon(dataSource, obj, IconType.NORMAL)
    }

    private fun getIcon(dataSource: FactorioDataSource, obj: FactorioObject, iconType: IconType): Icon? {
        val cache = cacheByType(iconType)
        val iconSpec = obj.iconSpec
        if (iconSpec.isNotEmpty()) {
            val simpleSprite = (iconSpec.size == 1 && iconSpec[0].isSimple())
            val cachedIcon = cache[iconSpec[0]]
            if (simpleSprite && cachedIcon != null) {
                return cachedIcon
            }

            return try {
                val icon = createIconFromSpec(dataSource, iconType, *iconSpec.toTypedArray())
                if (simpleSprite) {
                    cache[iconSpec[0]] = icon
                }
                icon
            } catch (e: Exception) {
                null
            }
        } else if (obj is Recipe) {
            val mainProduct = obj.mainProduct
            if (mainProduct != null) {
                return createIconFromSpec(dataSource, iconType, *mainProduct.iconSpec.toTypedArray())
            }
        }

        return null
    }

    private fun createIconFromSpec(
        dataSource: FactorioDataSource,
        iconType: IconType,
        vararg spec: FactorioIconPart
    ): Icon? {
        val cache = cacheByType(iconType)
        val images = arrayListOf<Icon>()
        val applyScale = (spec.size > 1)
        for (icon in spec) {
            val (mod, path) = ApplicationManager.getApplication().runReadAction<Pair<String, String>> {
                dataSource.resolveModPath("", icon.path)
            }
            var image = cache[icon]
            if (image == null) {
                val imageSource = ApplicationManager.getApplication().runReadAction<ByteArray?> {
                    dataSource.readModFile(mod, path)
                }
                if (imageSource == null) {
                    cache[icon] = null
                } else {
                    val rawImage = Toolkit.getDefaultToolkit().createImage(imageSource)
                    if (rawImage != null) {
                        image = rawImage.createIconWithSpec(icon, applyScale, iconType)
                    }
                    cache[icon] = image
                }
            }
            if (image != null) {
                images.add(image)
            }
        }

        val resizedImages = images.map { IconUtil.resizeSquared(it, ICON_SIZE) }
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

    private fun cacheByType(iconType: IconType) = when (iconType) {
        IconType.SMALL -> smallIconCache
        IconType.NORMAL -> normalIconCache
    }

    private fun Image.createIconWithSpec(spec: FactorioIconPart, applyScale: Boolean, iconType: IconType): Icon {
        val tempIcon = IconUtil.createImageIcon(this)
        val size = min(tempIcon.iconWidth, tempIcon.iconHeight)
        val scaledImage = ImageUtil.createImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                val targetSize = if (applyScale) {
                    (ICON_SIZE * spec.scale).toInt()
                } else {
                    ICON_SIZE
                }
                val targetRect = if (spec.x != 0f || spec.y != 0f) {
                    val targetWidth = (ICON_SHIFT_UNIT * (spec.size * spec.scale)).toInt()
                    val basePosition = (ICON_SIZE - targetWidth) / 2f
                    val x = (basePosition + (spec.x * ICON_SHIFT_UNIT)).roundToInt()
                    val y = (basePosition + (spec.y * ICON_SHIFT_UNIT)).roundToInt()
                    Rectangle(x, y, targetWidth, targetWidth)
                } else {
                    val basePosition = ((ICON_SIZE - targetSize) / 2f).roundToInt()
                    Rectangle(basePosition, basePosition, targetSize, targetSize)
                }

                val scaledRect = when (iconType) {
                    IconType.SMALL -> {
                        val x = targetRect.x + ((targetRect.centerX - targetRect.x) / 2).roundToInt()
                        val y = targetRect.y + ((targetRect.centerY - targetRect.y) / 2).roundToInt()
                        Rectangle(x, y, targetRect.width / 2, targetRect.height / 2)
                    }

                    IconType.NORMAL -> {
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

    enum class IconType {
        SMALL, NORMAL;
    }
}
