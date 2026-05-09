package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class JisonFormatAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile ?: return
        if (!isJisonLikeFile(file.name, file.fileType)) {
            return
        }

        // Emit on leaf elements so annotation ranges always belong to current element.
        if (element.firstChild != null) {
            return
        }

        val session = holder.currentAnnotationSession
        val issues = session.getUserData(ISSUES_KEY) ?: findIssues(file.text).also {
            session.putUserData(ISSUES_KEY, it)
        }
        val reported = session.getUserData(REPORTED_KEY) ?: mutableSetOf<String>().also {
            session.putUserData(REPORTED_KEY, it)
        }

        val text = file.text
        val elementRange = element.textRange
        for (issue in issues) {
            if (!rangesOverlap(issue.startOffset, issue.endOffset, elementRange.startOffset, elementRange.endOffset)) {
                continue
            }

            val reportKey = "${issue.startOffset}:${issue.endOffset}:${issue.message}"
            if (!reported.add(reportKey)) {
                continue
            }

            val start = issue.startOffset.coerceIn(0, text.length)
            val end = issue.endOffset.coerceIn(start + 1, text.length)
            holder.newAnnotation(HighlightSeverity.ERROR, issue.message)
                .range(TextRange(start, end))
                .create()
        }
    }

    data class Issue(val startOffset: Int, val endOffset: Int, val message: String)

    companion object {
        private val ISSUES_KEY = Key.create<List<Issue>>("jison.format.annotator.issues")
        private val REPORTED_KEY = Key.create<MutableSet<String>>("jison.format.annotator.reported")

        private const val COMMENT_DIRECTIVE = "JISON_COMMENT_FLAG"
        private const val OPEN_BRACE_MESSAGE = "'{' must be at line end"
        private const val CLOSE_BRACE_MESSAGE = "'}' must be on its own line"
        private const val INVALID_COMMENT_FLAG_MESSAGE = "JISON_COMMENT_FLAG only supports '#' or '//'"
        private const val UNMATCHED_OPEN_BRACE_MESSAGE = "Unclosed '{' block"
        private const val UNMATCHED_CLOSE_BRACE_MESSAGE = "Unmatched '}'"
        private const val MULTIPLE_ITEMS_MESSAGE = "Each config item must be on a new line"
        private const val MISSING_COLON_MESSAGE = "Key must be followed by ':'"
        private const val DUPLICATE_KEY_MESSAGE = "Duplicate key in the same level"

        private fun isJisonLikeFile(fileName: String, fileType: com.intellij.openapi.fileTypes.FileType): Boolean {
            if (fileType == JisonFileType || fileType == JisonSchemaFileType) {
                return true
            }

            val lower = fileName.lowercase()
            return lower.endsWith(".jison") || lower.endsWith(".jisonschema") || lower.endsWith(".jison.schema")
        }

        fun findIssues(text: String): List<Issue> {
            val issues = mutableListOf<Issue>()
            var commentFlag = "#"
            var openBraceDepth = 0
            var lastOpenBraceOffset = -1
            var lastOpenBraceIssueRange: IntRange? = null
            val seenKeysByDepth = mutableMapOf<Int, MutableSet<String>>()

            var lineStart = 0
            while (lineStart <= text.length) {
                val lineEnd = text.indexOf('\n', lineStart).let { if (it >= 0) it else text.length }
                val rawLine = text.substring(lineStart, lineEnd)

                val commentStart = findCommentStart(rawLine, commentFlag)
                val codePart = if (commentStart >= 0) rawLine.substring(0, commentStart) else rawLine
                val trimmedRight = codePart.trimEnd()
                val firstNonWhitespaceOffset = codePart.indexOfFirst { !it.isWhitespace() }
                val lineIssueRange = createLineIssueRange(lineStart, codePart)
                val directive = parseDirective(trimmedRight)

                if (trimmedRight.isNotBlank()) {
                    validateOpenBrace(trimmedRight, lineIssueRange, issues)
                    validateCloseBrace(trimmedRight, lineIssueRange, issues)
                    validateSingleConfigItemPerLine(trimmedRight, lineIssueRange, issues)
                    validateMissingColon(trimmedRight, firstNonWhitespaceOffset, directive, lineIssueRange, issues)
                    validateDuplicateKey(trimmedRight, openBraceDepth, directive, seenKeysByDepth, lineIssueRange, issues)
                }

                if (directive.isDirective && directive.isInvalid && firstNonWhitespaceOffset >= 0) {
                    issues += createIssue(lineIssueRange, INVALID_COMMENT_FLAG_MESSAGE)
                }
                if (directive.newFlag != null) {
                    commentFlag = directive.newFlag
                }

                var index = 0
                while (index < codePart.length) {
                    val ch = codePart[index]
                    if (ch == '{' && !isEscaped(codePart, index)) {
                        openBraceDepth++
                        lastOpenBraceOffset = lineStart + index
                        lastOpenBraceIssueRange = lineIssueRange
                    } else if (ch == '}' && !isEscaped(codePart, index)) {
                        openBraceDepth--
                        if (openBraceDepth < 0) {
                            issues += createIssue(lineIssueRange, UNMATCHED_CLOSE_BRACE_MESSAGE)
                            openBraceDepth = 0
                            seenKeysByDepth.keys.removeAll { it > 0 }
                        } else {
                            seenKeysByDepth.remove(openBraceDepth + 1)
                        }
                    }
                    index++
                }

                if (lineEnd == text.length) {
                    break
                }
                lineStart = lineEnd + 1
            }

            if (openBraceDepth > 0) {
                issues += lastOpenBraceIssueRange?.let { createIssue(it, UNMATCHED_OPEN_BRACE_MESSAGE) }
                    ?: Issue(lastOpenBraceOffset.coerceAtLeast(0), (lastOpenBraceOffset + 1).coerceAtLeast(1), UNMATCHED_OPEN_BRACE_MESSAGE)
            }

            return issues
        }

        private fun validateOpenBrace(line: String, lineIssueRange: IntRange?, issues: MutableList<Issue>) {
            for (index in line.indices) {
                if (line[index] == '{' && !isEscaped(line, index)) {
                    val trailing = line.substring(index + 1)
                    if (trailing.any { !it.isWhitespace() }) {
                        issues += createIssue(lineIssueRange, OPEN_BRACE_MESSAGE)
                    }
                }
            }
        }

        private fun validateCloseBrace(line: String, lineIssueRange: IntRange?, issues: MutableList<Issue>) {
            val trimmed = line.trim()
            for (index in line.indices) {
                if (line[index] == '}' && !isEscaped(line, index) && trimmed != "}") {
                    issues += createIssue(lineIssueRange, CLOSE_BRACE_MESSAGE)
                    return
                }
            }
        }

        private fun validateSingleConfigItemPerLine(line: String, lineIssueRange: IntRange?, issues: MutableList<Issue>) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("$COMMENT_DIRECTIVE:")) {
                return
            }

            val firstColon = findUnescapedColon(line, 0)
            if (firstColon < 0) {
                return
            }

            var searchFrom = firstColon + 1
            while (searchFrom < line.length) {
                val colonIndex = findUnescapedColon(line, searchFrom)
                if (colonIndex < 0) {
                    return
                }

                // Allow URL schemes in values, for example: key: https://example.com
                if (isUrlSchemeColon(line, colonIndex)) {
                    searchFrom = colonIndex + 1
                    continue
                }

                val whitespaceBeforeColon = line.lastIndexOfAny(charArrayOf(' ', '\t'), colonIndex - 1)
                if (whitespaceBeforeColon > firstColon) {
                    val keyCandidate = line.substring(whitespaceBeforeColon + 1, colonIndex)
                    if (keyCandidate.isNotBlank() && !keyCandidate.contains('/')) {
                        issues += createIssue(lineIssueRange, MULTIPLE_ITEMS_MESSAGE)
                        return
                    }
                }

                searchFrom = colonIndex + 1
            }
        }

        private fun isUrlSchemeColon(line: String, colonIndex: Int): Boolean {
            return colonIndex + 2 < line.length && line[colonIndex + 1] == '/' && line[colonIndex + 2] == '/'
        }

        private fun validateMissingColon(
            line: String,
            firstNonWhitespaceOffset: Int,
            directive: DirectiveResult,
            lineIssueRange: IntRange?,
            issues: MutableList<Issue>
        ) {
            if (directive.isDirective) {
                return
            }

            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed == "{" || trimmed == "}") {
                return
            }

            if (findUnescapedColon(line, 0) >= 0) {
                return
            }

            if (firstNonWhitespaceOffset >= 0) {
                issues += createIssue(lineIssueRange, MISSING_COLON_MESSAGE)
            }
        }

        private fun validateDuplicateKey(
            line: String,
            depth: Int,
            directive: DirectiveResult,
            seenKeysByDepth: MutableMap<Int, MutableSet<String>>,
            lineIssueRange: IntRange?,
            issues: MutableList<Issue>
        ) {
            if (directive.isDirective) {
                return
            }

            val firstColon = findUnescapedColon(line, 0)
            if (firstColon < 0) {
                return
            }

            val key = line.substring(0, firstColon).trim()
            if (key.isBlank()) {
                return
            }

            val keysInDepth = seenKeysByDepth.getOrPut(depth) { mutableSetOf() }
            if (!keysInDepth.add(key)) {
                issues += createIssue(lineIssueRange, DUPLICATE_KEY_MESSAGE)
            }
        }

        private fun createLineIssueRange(lineStart: Int, codePart: String): IntRange? {
            val firstNonWhitespace = codePart.indexOfFirst { !it.isWhitespace() }
            if (firstNonWhitespace < 0) {
                return null
            }

            val lastNonWhitespace = codePart.indexOfLast { !it.isWhitespace() }
            if (lastNonWhitespace < firstNonWhitespace) {
                return null
            }

            return (lineStart + firstNonWhitespace)..(lineStart + lastNonWhitespace + 1)
        }

        private fun createIssue(range: IntRange?, message: String): Issue {
            if (range == null) {
                return Issue(0, 1, message)
            }
            return Issue(range.first, range.last, message)
        }

        private fun rangesOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
            return start1 < end2 && start2 < end1
        }

        private fun findUnescapedColon(text: String, from: Int): Int {
            var index = from
            while (index < text.length) {
                if (text[index] == ':' && !isEscaped(text, index)) {
                    return index
                }
                index++
            }
            return -1
        }

        private fun parseDirective(line: String): DirectiveResult {
            val trimmed = line.trim()
            if (!trimmed.startsWith("$COMMENT_DIRECTIVE:")) {
                return DirectiveResult(isDirective = false, newFlag = null, isInvalid = false)
            }

            val value = trimmed.substringAfter(':').trim()
            val newFlag = when (value) {
                "#", "//" -> value
                else -> null
            }
            return DirectiveResult(isDirective = true, newFlag = newFlag, isInvalid = newFlag == null)
        }

        private data class DirectiveResult(
            val isDirective: Boolean,
            val newFlag: String?,
            val isInvalid: Boolean
        )

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
}


