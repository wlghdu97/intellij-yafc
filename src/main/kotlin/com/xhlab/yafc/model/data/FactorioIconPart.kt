package com.xhlab.yafc.model.data

data class FactorioIconPart(
    val path: String,
    val size: Float = 32f,
    val x: Float = 0f,
    val y: Float = 0f,
    val r: Float = 1f,
    val g: Float = 1f,
    val b: Float = 1f,
    val a: Float = 1f,
    val scale: Float = 1f,
    val mipmaps: Int = 1
) {
    fun isSimple(): Boolean {
        return (x == 0f && y == 0f && r == 1f && g == 1f && b == 1f && a == 1f && scale == 1f)
    }
}
