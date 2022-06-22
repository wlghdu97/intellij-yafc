package com.xhlab.yafc.lang

import com.intellij.json.JsonParserDefinition
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType

class YAFCParserDefinition : JsonParserDefinition() {

    override fun getFileNodeType(): IFileElementType = FILE

    override fun createFile(fileViewProvider: FileViewProvider): PsiFile {
        return YAFCFile(fileViewProvider)
    }

    companion object {
        @JvmField
        val FILE = IFileElementType(YAFCLanguage)
    }
}
