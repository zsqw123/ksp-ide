package com.zsu.ksp.ide

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.Messages
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

abstract class AnnotationLineMaker(private val processor: KspAnnotationProcessor) :
    LineMarkerProvider {
    private val annotationFqn = processor.annotationFqn
    private val annotationShort = annotationFqn.substringAfterLast('.')
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    class AnnotationMarkerInfo(
        element: PsiElement, message: String,
        navHandler: GutterIconNavigationHandler<PsiElement>? = null
    ) : LineMarkerInfo<PsiElement>(
        element,
        element.textRange,
        KotlinIcons.SUSPEND_CALL,
        { message },
        navHandler,
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
            it.findAnnotation(annotationFqn) != null
        }
        for (uDeclaration in annotatedDeclarations) {
            val uAnnotation = uDeclaration.findAnnotation(annotationFqn) ?: continue
            val sourcePsi = uDeclaration.sourcePsi ?: continue
            val annotationSourcePsi = uAnnotation.uastAnchor?.sourcePsi ?: continue
            result += AnnotationMarkerInfo(annotationSourcePsi, "Fast Generate KSP") { _, _ ->
                val kspRoot = readKspPath(sourcePsi)
                if (kspRoot == null) {
                    Messages.showErrorDialog(
                        sourcePsi.project,
                        "No ksp generate path be found! Please configure ksp_generate_dir in " +
                            "module root gradle.properties",
                        "Fast Generate KSP",
                    )
                    return@AnnotationMarkerInfo
                }
                processor.processAnnotated(uDeclaration, kspRoot)
            }
        }
    }
}
