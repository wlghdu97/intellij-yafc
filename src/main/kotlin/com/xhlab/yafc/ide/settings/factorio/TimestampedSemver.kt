package com.xhlab.yafc.ide.settings.factorio

import com.intellij.util.text.SemVer

data class TimestampedSemver(
    val version: SemVer?,
    val lastModified: Long,
    val loadedFromXml: Boolean
)
