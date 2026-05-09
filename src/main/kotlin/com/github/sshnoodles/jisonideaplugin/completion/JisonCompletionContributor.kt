package com.github.sshnoodles.jisonideaplugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.github.sshnoodles.jisonideaplugin.lang.JisonLexer

class JisonCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    result.addElement(LookupElementBuilder.create(JisonLexer.COMMENT_FLAG_DIRECTIVE))
                    result.addElement(LookupElementBuilder.create("${JisonLexer.COMMENT_FLAG_DIRECTIVE}:#"))
                    result.addElement(LookupElementBuilder.create("${JisonLexer.COMMENT_FLAG_DIRECTIVE}://"))
                }
            }
        )
    }
}

