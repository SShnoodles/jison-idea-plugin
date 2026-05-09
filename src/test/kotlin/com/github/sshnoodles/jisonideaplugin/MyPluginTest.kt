package com.github.sshnoodles.jisonideaplugin

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.github.sshnoodles.jisonideaplugin.lang.JisonFormatAnnotator
import com.github.sshnoodles.jisonideaplugin.lang.JisonLexer
import com.github.sshnoodles.jisonideaplugin.lang.JisonTypes

class MyPluginTest : BasePlatformTestCase() {

    fun testLexerSwitchesCommentFlag() {
        val text = """
            # first comment
            JISON_COMMENT_FLAG://
            key#part:value // second comment
        """.trimIndent()

        val lexer = JisonLexer()
        lexer.start(text)

        var directiveCount = 0
        var commentCount = 0
        var keyCount = 0

        while (lexer.tokenType != null) {
            when (lexer.tokenType) {
                JisonTypes.DIRECTIVE -> directiveCount++
                JisonTypes.COMMENT -> commentCount++
                JisonTypes.KEY -> keyCount++
            }
            lexer.advance()
        }

        assertEquals(1, directiveCount)
        assertEquals(2, commentCount)
        assertTrue(keyCount >= 1)
    }

    fun testLexerHonorsEscapedCommentCharacter() {
        val text = "escaped\\#key:value\\#raw # comment"
        val lexer = JisonLexer()
        lexer.start(text)

        var commentCount = 0
        var keyCount = 0
        var valueCount = 0

        while (lexer.tokenType != null) {
            when (lexer.tokenType) {
                JisonTypes.COMMENT -> commentCount++
                JisonTypes.KEY -> keyCount++
                JisonTypes.VALUE -> valueCount++
            }
            lexer.advance()
        }

        assertEquals(1, commentCount)
        assertEquals(1, keyCount)
        assertEquals(1, valueCount)
    }

    fun testLexerSupportsValueWithSpacesAfterColon() {
        val text = "key:value with spaces"
        val lexer = JisonLexer()
        lexer.start(text)

        val tokens = mutableListOf<Pair<com.intellij.psi.tree.IElementType?, String>>()
        while (lexer.tokenType != null) {
            val tokenText = text.substring(lexer.tokenStart, lexer.tokenEnd)
            tokens += lexer.tokenType to tokenText
            lexer.advance()
        }

        assertContainsElements(tokens.map { it.first }, JisonTypes.KEY, JisonTypes.COLON, JisonTypes.VALUE)
        assertTrue(tokens.any { it.first == JisonTypes.VALUE && it.second == "value with spaces" })
    }

    fun testLexerRestoresCommentFlagAcrossRestart() {
        val text = """
            JISON_COMMENT_FLAG://
            // comment with new flag
        """.trimIndent()

        val restartOffset = text.indexOf("// comment with new flag")
        val lexer = JisonLexer()
        lexer.start(text)

        var restartState = 0
        while (lexer.tokenType != null) {
            if (lexer.tokenStart == restartOffset) {
                restartState = lexer.state
                break
            }
            lexer.advance()
        }

        val restartedLexer = JisonLexer()
        restartedLexer.start(text, restartOffset, text.length, restartState)

        assertEquals(JisonTypes.COMMENT, restartedLexer.tokenType)
    }

    fun testFormatDiagnosticsDetectInlineBraces() {
        val issues = JisonFormatAnnotator.findIssues("root:{key:value}")
        val messages = issues.map { it.message }

        assertContainsElements(messages, "'{' must be at line end", "'}' must be on its own line")
    }

    fun testFormatDiagnosticsRespectsCommentFlagSwitch() {
        val text = """
            root:{
              key:value
            }
            JISON_COMMENT_FLAG:// # switch flag after this line
            // comment with new flag
            good:{
              value:http://example.com
            }
        """.trimIndent()

        val issues = JisonFormatAnnotator.findIssues(text)
        assertTrue(issues.isEmpty())
    }

    fun testFormatDiagnosticsDetectInvalidCommentDirectiveValue() {
        val issues = JisonFormatAnnotator.findIssues("JISON_COMMENT_FLAG:--")
        assertContainsElements(issues.map { it.message }, "JISON_COMMENT_FLAG only supports '#' or '//'" )
    }

    fun testFormatDiagnosticsDetectUnmatchedBraces() {
        val issues = JisonFormatAnnotator.findIssues("root:{\n  key:value\n")
        assertContainsElements(issues.map { it.message }, "Unclosed '{' block")

        val closeIssues = JisonFormatAnnotator.findIssues("}")
        assertContainsElements(closeIssues.map { it.message }, "Unmatched '}'")
    }

    fun testFormatDiagnosticsDetectMultipleItemsOnSameLine() {
        val issues = JisonFormatAnnotator.findIssues("root:{\n  key1:value1 key2:value2\n}")
        assertContainsElements(issues.map { it.message }, "Each config item must be on a new line")
    }

    fun testFormatDiagnosticsAllowUrlValueWithColon() {
        val issues = JisonFormatAnnotator.findIssues("key1:https://www.google.com")
        assertTrue(issues.isEmpty())
    }

    fun testFormatDiagnosticsAllowUrlValueWithSpaceAfterColon() {
        val issues = JisonFormatAnnotator.findIssues("key: https://www.google.com")
        assertTrue(issues.isEmpty())
    }

    fun testFormatDiagnosticsAllowValueWithSpaces() {
        val issues = JisonFormatAnnotator.findIssues("key1:value with spaces")
        assertTrue(issues.isEmpty())
    }

    fun testFormatDiagnosticsDetectMissingColonAfterKey() {
        val issues = JisonFormatAnnotator.findIssues("root:{\n  key1\n}")
        assertContainsElements(issues.map { it.message }, "Key must be followed by ':'")
    }

    fun testFormatDiagnosticsAllowEmptyValue() {
        val issues = JisonFormatAnnotator.findIssues("key1:")
        assertTrue(issues.isEmpty())
    }

    fun testFormatDiagnosticsDetectDuplicateKeysInSameLevel() {
        val text = """
            root:{
              key1:value1
              key1:value2
            }
        """.trimIndent()
        val issues = JisonFormatAnnotator.findIssues(text)
        assertContainsElements(issues.map { it.message }, "Duplicate key in the same level")
    }

    fun testFormatDiagnosticsAllowSameKeyInNestedLevel() {
        val text = """
            root:{
              key1:value1
              child:{
                key1:value2
              }
            }
        """.trimIndent()
        val issues = JisonFormatAnnotator.findIssues(text)
        assertFalse(issues.any { it.message == "Duplicate key in the same level" })
    }

    fun testAnnotatorProducesWarningsInEditor() {
        val psiFile = myFixture.configureByText("bad.jison", "root:{key:value}")
        val infos = myFixture.doHighlighting(HighlightSeverity.ERROR)
        val descriptions = infos.mapNotNull { it.description }
        assertContainsElements(
            "fileType=${psiFile.fileType.name}, language=${psiFile.language.id}, descriptions=$descriptions",
            descriptions,
            "'{' must be at line end",
            "'}' must be on its own line"
        )

        val openBraceInfo = infos.firstOrNull { it.description == "'{' must be at line end" }
        assertNotNull("Expected full-line highlight info for '{' rule", openBraceInfo)
        assertTrue(
            "Expected error range to span more than one character, but was ${openBraceInfo!!.startOffset}..${openBraceInfo.endOffset}",
            openBraceInfo.endOffset - openBraceInfo.startOffset > 1
        )
    }
}
