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
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.toUElement
import java.io.File
import javax.swing.Icon

abstract class AnnotationLineMaker(private val processor: KspAnnotationProcessor) :
    LineMarkerProvider {
    private val annotationFqn = processor.annotationFqn
    private val annotationShort = annotationFqn.substringAfterLast('.')
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null
    abstract fun icon(): Icon

    private class AnnotationMarkerInfo(
        element: PsiElement, message: String, icon: Icon,
        navHandler: GutterIconNavigationHandler<PsiElement>? = null
    ) : LineMarkerInfo<PsiElement>(
        element, element.textRange, icon,
        { message }, navHandler, GutterIconRenderer.Alignment.RIGHT,
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
            val annotationSourcePsi = uAnnotation.uastAnchor?.sourcePsi ?: continue
            result += AnnotationMarkerInfo(
                annotationSourcePsi, "Fast Generate KSP", icon(),
            ) { _, _ ->
                val kspRoot = findKspRoot(uDeclaration) ?: return@AnnotationMarkerInfo
                if (processor.requireProcessAllAnnotated(uDeclaration)) {
                    val scope = annotationSourcePsi.module?.getModuleScope(false)
                        ?: annotationSourcePsi.resolveScope
                    val allAnnotated = EdenSearch.getAnnotatedElements(
                        annotationSourcePsi.project, annotationFqn, scope,
                    ).asSequence().mapNotNull { it.toUElement(UDeclaration::class.java) }
                    processor.processAllAnnotated(allAnnotated, kspRoot)
                } else {
                    processor.processAnnotated(uDeclaration, kspRoot)
                }
            }
        }
    }

    protected open fun findKspRoot(context: UDeclaration): File? {
        val sourcePsi = context.sourcePsi ?: return null
        val kspRoot = readKspPath(sourcePsi)
        if (kspRoot == null) {
            Messages.showErrorDialog(
                sourcePsi.project,
                "No ksp generate path be found! Please configure ksp_generate_dir in " +
                    "module root gradle.properties",
                "Fast Generate KSP",
            )
            return null
        }
        return kspRoot
    }
}
