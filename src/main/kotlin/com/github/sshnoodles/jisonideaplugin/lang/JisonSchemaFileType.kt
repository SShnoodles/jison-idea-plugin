package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object JisonSchemaFileType : LanguageFileType(JisonSchemaLanguage) {
    override fun getName() = "Jison Schema"

    override fun getDescription() = "Jison schema file"

    override fun getDefaultExtension() = "jisonschema"

    override fun getIcon(): Icon = AllIcons.FileTypes.Config
}

