package com.xhlab.yafc.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class YAFCFile constructor(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, YAFCLanguage) {

    override fun getFileType() = YAFCFileType
}
