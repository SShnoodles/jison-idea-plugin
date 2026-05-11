package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.psi.PsiFile
import kotlin.math.max

object JisonFormatter {
    private const val COMMENT_DIRECTIVE = "JISON_COMMENT_FLAG"

    fun formatText(text: String): String {
        val source = text.replace("\r\n", "\n").replace('\r', '\n')
        val keepTrailingNewLine = source.endsWith("\n")
        val lines = source.split('\n')

        val result = ArrayList<String>(lines.size)
        var depth = 0
        var commentFlag = "#"

        for (line in lines) {
            val commentStart = findCommentStart(line, commentFlag)
            val codePart = if (commentStart >= 0) line.substring(0, commentStart) else line
            val commentPart = if (commentStart >= 0) line.substring(commentStart).trim() else ""
            val code = codePart.trim()
            val leadingCloseBraces = countLeadingCloseBraces(code)

            if (leadingCloseBraces > 0) {
                depth = max(0, depth - leadingCloseBraces)
            }

            val indent = "  ".repeat(depth)

            val emptyObjectKey = parseEmptyObjectKey(code)
            if (emptyObjectKey != null) {
                val openLine = if (commentPart.isEmpty()) {
                    indent + "$emptyObjectKey:{"
                } else {
                    indent + "$emptyObjectKey:{ $commentPart"
                }
                result += openLine.trimEnd()
                result += (indent + "}")
                continue
            }

            val normalizedCode = normalizeCode(code)
            val normalizedLine = when {
                normalizedCode.isEmpty() && commentPart.isEmpty() -> ""
                normalizedCode.isEmpty() -> indent + commentPart
                commentPart.isEmpty() -> indent + normalizedCode
                else -> indent + normalizedCode + " " + commentPart
            }
            result += normalizedLine.trimEnd()

            parseDirectiveValue(normalizedCode)?.let { commentFlag = it }

            depth += countUnescaped(normalizedCode, '{')
            depth -= max(0, countUnescaped(normalizedCode, '}') - leadingCloseBraces)
            if (depth < 0) {
                depth = 0
            }
        }

        val joined = result.joinToString("\n")
        return if (keepTrailingNewLine && !joined.endsWith("\n")) "$joined\n" else joined
    }

    fun isJisonLikeFile(file: PsiFile): Boolean {
        if (file.fileType == JisonFileType || file.fileType == JisonSchemaFileType) {
            return true
        }

        val lower = file.name.lowercase()
        return lower.endsWith(".jison") || lower.endsWith(".jisonschema") || lower.endsWith(".jison.schema")
    }

    private fun normalizeCode(code: String): String {
        if (code.isBlank()) {
            return ""
        }

        val firstColon = findUnescapedColon(code)
        if (firstColon < 0) {
            return code
        }

        val key = code.substring(0, firstColon).trim()
        val value = code.substring(firstColon + 1).trim()

        if (key == COMMENT_DIRECTIVE) {
            return if (value.isEmpty()) "$COMMENT_DIRECTIVE:" else "$COMMENT_DIRECTIVE:$value"
        }

        if (value == "{") {
            return "$key:{"
        }

        return if (value.isEmpty()) "$key:" else "$key: $value"
    }

    private fun parseDirectiveValue(code: String): String? {
        val firstColon = findUnescapedColon(code)
        if (firstColon < 0) {
            return null
        }

        val key = code.substring(0, firstColon).trim()
        if (key != COMMENT_DIRECTIVE) {
            return null
        }

        return when (code.substring(firstColon + 1).trim()) {
            "#", "//" -> code.substring(firstColon + 1).trim()
            else -> null
        }
    }

    private fun parseEmptyObjectKey(code: String): String? {
        val firstColon = findUnescapedColon(code)
        if (firstColon < 0) {
            return null
        }

        val key = code.substring(0, firstColon).trim()
        if (key.isEmpty() || key == COMMENT_DIRECTIVE) {
            return null
        }

        return if (code.substring(firstColon + 1).trim() == "{}") key else null
    }

    private fun findCommentStart(line: String, commentFlag: String): Int {
        if (commentFlag == "#") {
            for (index in line.indices) {
                if (line[index] == '#' && !isEscaped(line, index)) {
                    return index
                }
            }
            return -1
        }

        var index = 0
        while (index < line.length - 1) {
            if (line[index] == '/' && line[index + 1] == '/' && !isEscaped(line, index)) {
                return index
            }
            index++
        }
        return -1
    }

    private fun countUnescaped(text: String, target: Char): Int {
        var count = 0
        for (index in text.indices) {
            if (text[index] == target && !isEscaped(text, index)) {
                count++
            }
        }
        return count
    }

    private fun countLeadingCloseBraces(text: String): Int {
        var count = 0
        while (count < text.length && text[count] == '}' && !isEscaped(text, count)) {
            count++
        }
        return count
    }

    private fun findUnescapedColon(text: String): Int {
        for (index in text.indices) {
            if (text[index] == ':' && !isEscaped(text, index)) {
                return index
            }
        }
        return -1
    }

    private fun isEscaped(text: String, index: Int): Boolean {
        if (index <= 0) {
            return false
        }

        var slashCount = 0
        var i = index - 1
        while (i >= 0 && text[i] == '\\') {
            slashCount++
            i--
        }
        return slashCount % 2 == 1
    }
}


