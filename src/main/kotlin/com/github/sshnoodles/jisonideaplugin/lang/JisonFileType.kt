package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object JisonFileType : LanguageFileType(JisonLanguage) {
    override fun getName() = "Jison"

    override fun getDescription() = "Jison configuration file"

    override fun getDefaultExtension() = "jison"

    override fun getIcon(): Icon = AllIcons.FileTypes.Config
}

