package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class JisonSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = JisonLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        JisonTypes.COMMENT -> pack(COMMENT)
        JisonTypes.DIRECTIVE -> pack(DIRECTIVE)
        JisonTypes.KEY -> pack(KEY)
        JisonTypes.VALUE -> pack(VALUE)
        JisonTypes.COLON -> pack(COLON)
        JisonTypes.LBRACE, JisonTypes.RBRACE -> pack(BRACES)
        JisonTypes.ESCAPE -> pack(ESCAPE)
        JisonTypes.BAD_CHARACTER, TokenType.BAD_CHARACTER -> pack(BAD_CHARACTER)
        else -> emptyArray()
    }

    companion object {
        val COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "JISON_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        val DIRECTIVE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "JISON_DIRECTIVE",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        val KEY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "JISON_KEY",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD
        )
        val VALUE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "JISON_VALUE",
            DefaultLanguageHighlighterColors.STRING
        )
        val COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "JISON_COLON",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        val BRACES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "JISON_BRACES",
            DefaultLanguageHighlighterColors.BRACES
        )
        val ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "JISON_ESCAPE",
            DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE
        )
        val BAD_CHARACTER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "JISON_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )
    }
}

