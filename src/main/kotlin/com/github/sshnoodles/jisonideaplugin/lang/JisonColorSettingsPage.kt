package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

open class BaseJisonColorSettingsPage(private val displayName: String) : ColorSettingsPage {
    override fun getDisplayName(): String = displayName

    override fun getIcon(): Icon = AllIcons.FileTypes.Config

    override fun getHighlighter(): SyntaxHighlighter = JisonSyntaxHighlighter()

    override fun getDemoText(): String = """
        # default comment flag
        root:{
          key1:value1
          key2\#escaped:value2\#escaped # escaped # in key and value
        }

        JISON_COMMENT_FLAG:// # switch comment style
        // now // starts comments
        other:{
          key1#raw:value
        }
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor("Comment", JisonSyntaxHighlighter.COMMENT),
        AttributesDescriptor("Directive", JisonSyntaxHighlighter.DIRECTIVE),
        AttributesDescriptor("Key", JisonSyntaxHighlighter.KEY),
        AttributesDescriptor("Value", JisonSyntaxHighlighter.VALUE),
        AttributesDescriptor("Colon", JisonSyntaxHighlighter.COLON),
        AttributesDescriptor("Braces", JisonSyntaxHighlighter.BRACES),
        AttributesDescriptor("Escape", JisonSyntaxHighlighter.ESCAPE),
        AttributesDescriptor("Bad character", JisonSyntaxHighlighter.BAD_CHARACTER)
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
}

class JisonColorSettingsPage : BaseJisonColorSettingsPage("Jison")

class JisonSchemaColorSettingsPage : BaseJisonColorSettingsPage("Jison Schema")

