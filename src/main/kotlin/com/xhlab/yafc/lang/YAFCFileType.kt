package com.xhlab.yafc.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object YAFCFileType : LanguageFileType(YAFCLanguage) {

    override fun getName() = "YAFC File"

    override fun getDescription() = "YAFC project file"

    override fun getDefaultExtension() = "yafc"

    override fun getIcon(): Icon = AllIcons.General.GearPlain
}
