package com.github.sshnoodles.jisonideaplugin.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.ExternalFormatProcessor

@Suppress("UnstableApiUsage")
class JisonExternalFormatProcessor : ExternalFormatProcessor {
    override fun activeForFile(source: PsiFile): Boolean {
        return JisonFormatter.isJisonLikeFile(source)
    }

    override fun format(
        source: PsiFile,
        range: TextRange,
        canChangeWhiteSpacesOnly: Boolean,
        quickFormat: Boolean,
        excludeRanges: Boolean,
        caretOffset: Int
    ): TextRange {
        val documentManager = PsiDocumentManager.getInstance(source.project)
        val document = documentManager.getDocument(source) ?: return range
        val formatted = JisonFormatter.formatText(document.text)
        if (formatted == document.text) {
            return range
        }

        document.setText(formatted)
        documentManager.commitDocument(document)
        return TextRange(0, formatted.length)
    }

    override fun indent(source: PsiFile, lineStartOffset: Int): String = ""

    override fun getId(): String = "jison.external.formatter"
}


