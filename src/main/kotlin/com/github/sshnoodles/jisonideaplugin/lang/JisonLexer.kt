package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class JisonLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var startOffset = 0
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var position = 0
    private var tokenType: IElementType? = null
    private var tokenState = 0

    private var commentFlag = "#"
    private var atLineStart = true
    private var seenFirstColonOnLine = false
    private var directiveOnLine = false

    override fun start(
        buffer: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int
    ) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.position = startOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.tokenType = null
        this.tokenState = 0

        if (startOffset == 0) {
            this.commentFlag = "#"
            this.atLineStart = true
            this.seenFirstColonOnLine = false
            this.directiveOnLine = false
        } else {
            decodeState(initialState)
        }

        advance()
    }

    override fun getState() = tokenState

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart() = tokenStart

    override fun getTokenEnd() = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd() = endOffset

    override fun advance() {
        if (position >= endOffset) {
            tokenType = null
            tokenStart = endOffset
            tokenEnd = endOffset
            tokenState = encodeState()
            return
        }

        tokenStart = position
        tokenState = encodeState()
        val current = buffer[position]

        if (seenFirstColonOnLine && !isLineBreakAt(position) && !matchesCommentFlag(position) && current != '{' && current != '}') {
            position = readValueToken(position)
            tokenEnd = position

            if (tokenStart == tokenEnd) {
                position++
                tokenEnd = position
                tokenType = JisonTypes.BAD_CHARACTER
            } else {
                val tokenText = buffer.subSequence(tokenStart, tokenEnd).toString()
                if (directiveOnLine) {
                    val trimmed = tokenText.trim()
                    if (trimmed == "#" || trimmed == "//") {
                        commentFlag = trimmed
                    }
                }
                tokenType = JisonTypes.VALUE
            }
            atLineStart = false
            return
        }

        if (current.isWhitespace()) {
            while (position < endOffset && buffer[position].isWhitespace()) {
                if (isLineBreakAt(position)) {
                    atLineStart = true
                    seenFirstColonOnLine = false
                    directiveOnLine = false
                }
                position++
            }
            tokenEnd = position
            tokenType = TokenType.WHITE_SPACE
            return
        }

        if (matchesCommentFlag(position)) {
            position = readUntilLineBreak(position)
            tokenEnd = position
            tokenType = JisonTypes.COMMENT
            atLineStart = false
            return
        }

        if (current == '\\' && position + 1 < endOffset && isEscapable(buffer[position + 1])) {
            position += 2
            tokenEnd = position
            tokenType = JisonTypes.ESCAPE
            atLineStart = false
            return
        }

        if (current == '{') {
            position++
            tokenEnd = position
            tokenType = JisonTypes.LBRACE
            atLineStart = false
            return
        }

        if (current == '}') {
            position++
            tokenEnd = position
            tokenType = JisonTypes.RBRACE
            atLineStart = false
            return
        }

        if (current == ':' && !seenFirstColonOnLine) {
            position++
            tokenEnd = position
            tokenType = JisonTypes.COLON
            seenFirstColonOnLine = true
            atLineStart = false
            return
        }

        position = readWord(position)
        tokenEnd = position

        if (tokenStart == tokenEnd) {
            position++
            tokenEnd = position
            tokenType = JisonTypes.BAD_CHARACTER
            atLineStart = false
            return
        }

        val tokenText = buffer.subSequence(tokenStart, tokenEnd).toString()
        tokenType = if (!seenFirstColonOnLine) {
            if (atLineStart && tokenText == COMMENT_FLAG_DIRECTIVE) {
                directiveOnLine = true
                JisonTypes.DIRECTIVE
            } else {
                JisonTypes.KEY
            }
        } else {
            if (directiveOnLine) {
                val trimmed = tokenText.trim()
                if (trimmed == "#" || trimmed == "//") {
                    commentFlag = trimmed
                }
            }
            JisonTypes.VALUE
        }
        atLineStart = false
    }

    private fun readUntilLineBreak(offset: Int): Int {
        var index = offset
        while (index < endOffset && !isLineBreakAt(index)) {
            index++
        }
        return index
    }

    private fun readWord(offset: Int): Int {
        var index = offset
        while (index < endOffset) {
            if (matchesCommentFlag(index)) {
                break
            }

            val ch = buffer[index]
            if (ch.isWhitespace() || ch == '{' || ch == '}') {
                break
            }

            if (ch == ':' && !seenFirstColonOnLine) {
                break
            }

            index++
        }
        return index
    }

    private fun readValueToken(offset: Int): Int {
        var index = offset
        while (index < endOffset) {
            if (isLineBreakAt(index) || matchesCommentFlag(index)) {
                break
            }

            val ch = buffer[index]
            if (ch == '{' || ch == '}') {
                break
            }

            index++
        }
        return index
    }

    private fun matchesCommentFlag(offset: Int): Boolean {
        if (isEscaped(offset)) {
            return false
        }

        return if (commentFlag == "#") {
            buffer[offset] == '#'
        } else {
            offset + 1 < endOffset && buffer[offset] == '/' && buffer[offset + 1] == '/'
        }
    }

    private fun isEscaped(offset: Int): Boolean = offset > startOffset && buffer[offset - 1] == '\\'

    private fun encodeState(): Int {
        var state = 0
        if (commentFlag == "//") {
            state = state or STATE_COMMENT_DOUBLE_SLASH
        }
        if (atLineStart) {
            state = state or STATE_AT_LINE_START
        }
        if (seenFirstColonOnLine) {
            state = state or STATE_SEEN_FIRST_COLON
        }
        if (directiveOnLine) {
            state = state or STATE_DIRECTIVE_ON_LINE
        }
        return state
    }

    private fun decodeState(state: Int) {
        commentFlag = if ((state and STATE_COMMENT_DOUBLE_SLASH) != 0) "//" else "#"
        atLineStart = (state and STATE_AT_LINE_START) != 0
        seenFirstColonOnLine = (state and STATE_SEEN_FIRST_COLON) != 0
        directiveOnLine = (state and STATE_DIRECTIVE_ON_LINE) != 0
    }

    private fun isLineBreakAt(offset: Int): Boolean {
        val ch = buffer[offset]
        return ch == '\n' || ch == '\r'
    }

    private fun isEscapable(ch: Char): Boolean = ch == '#' || ch == '/'

    companion object {
        const val COMMENT_FLAG_DIRECTIVE = "JISON_COMMENT_FLAG"

        private const val STATE_COMMENT_DOUBLE_SLASH = 1
        private const val STATE_AT_LINE_START = 1 shl 1
        private const val STATE_SEEN_FIRST_COLON = 1 shl 2
        private const val STATE_DIRECTIVE_ON_LINE = 1 shl 3
    }
}

