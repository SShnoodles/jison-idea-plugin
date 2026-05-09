package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType

class JisonTokenType(debugName: String) : IElementType(debugName, Language.ANY)

object JisonTypes {
    val COMMENT = JisonTokenType("JISON_COMMENT")
    val DIRECTIVE = JisonTokenType("JISON_DIRECTIVE")
    val KEY = JisonTokenType("JISON_KEY")
    val VALUE = JisonTokenType("JISON_VALUE")
    val COLON = JisonTokenType("JISON_COLON")
    val LBRACE = JisonTokenType("JISON_LBRACE")
    val RBRACE = JisonTokenType("JISON_RBRACE")
    val ESCAPE = JisonTokenType("JISON_ESCAPE")
    val BAD_CHARACTER = JisonTokenType("JISON_BAD_CHARACTER")
}

