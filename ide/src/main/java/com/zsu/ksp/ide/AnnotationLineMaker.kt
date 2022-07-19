package com.zsu.ksp.ide

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.impl.source.tree.ElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfTypes
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.toUElement

abstract class AnnotationLineMaker(private val annotationFqn: String) :
    LineMarkerProvider, UastAnnotationProcessor {
    private val annotationShort = annotationFqn.substringAfterLast('.')
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    class AnnotationMarkerInfo(element: PsiElement, message: String) : LineMarkerInfo<PsiElement>(
        element,
        element.textRange,
        KotlinIcons.SUSPEND_CALL,
        { message },
        null,
        GutterIconRenderer.Alignment.RIGHT,
    )

    @Override
    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val uDeclarations = elements.filter {
            (it.elementType == KtTokens.IDENTIFIER ||
                it.elementType == ElementType.IDENTIFIER) &&
                it.text.endsWith(annotationShort)
        }.mapNotNull {
            it.parentOfTypes(KtAnnotationEntry::class, PsiAnnotation::class)
                ?.parentOfTypes(KtModifierListOwner::class, PsiModifierListOwner::class)
                .toUElement()
        }.filterIsInstance<UDeclaration>()
        val annotatedDeclarations = uDeclarations.filter {
            it.findAnnotation(annotationFqn) == null
        }
        annotatedDeclarations.forEach { processAnnotated(it) }
        val uAnnotations = uDeclarations.mapNotNull {
            it.findAnnotation(annotationFqn)
        }
        for (uAnnotation in uAnnotations) {
            val sourcePsi = uAnnotation.uastAnchor?.sourcePsi ?: continue
            result.add(AnnotationMarkerInfo(sourcePsi, "Fast Generate KSP"))
        }
    }
}
