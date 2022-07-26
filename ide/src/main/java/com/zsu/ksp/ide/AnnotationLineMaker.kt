package com.zsu.ksp.ide

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfTypes
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
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
        val ktDeclarations = elements.filter {
            it.elementType == KtTokens.IDENTIFIER &&
                it.text.endsWith(annotationShort)
        }.mapNotNull {
            it.parentOfTypes(KtAnnotationEntry::class)?.parentOfTypes(KtModifierListOwner::class)
        }.filterIsInstance<KtDeclaration>()
        val annotatedDeclarations = ktDeclarations.filter { ktDeclaration ->
            ktDeclaration.annotationEntries.any { it.shortName?.asString() == annotationShort }
        }
        for (ktDeclaration in annotatedDeclarations) {
            val ktAnnotationEntry = ktDeclaration.findAnnotation(FqName(annotationFqn)) ?: continue
            val annotationSourcePsi = ktAnnotationEntry.atSymbol ?: continue
            result += AnnotationMarkerInfo(
                annotationSourcePsi, "Fast Generate KSP", icon(),
            ) { _, _ ->
                val kspRoot = findKspRoot(ktDeclaration) ?: return@AnnotationMarkerInfo
                if (processor.requireProcessAllAnnotated(ktDeclaration)) {
                    val scope = annotationSourcePsi.module?.getModuleScope(false)
                        ?: annotationSourcePsi.resolveScope
                    val allAnnotated = EdenSearch.getAnnotatedElements(
                        annotationSourcePsi.project, annotationFqn, scope,
                    )
                    processor.processAllAnnotated(allAnnotated, kspRoot)
                } else {
                    processor.processAnnotated(ktDeclaration, kspRoot)
                }
            }
        }
    }

    protected open fun findKspRoot(context: PsiElement): File? {
        val kspRoot = readKspPath(context)
        if (kspRoot == null) {
            Messages.showErrorDialog(
                context.project,
                "No ksp generate path be found! Please configure ksp_generate_dir in " +
                    "module root gradle.properties",
                "Fast Generate KSP",
            )
            return null
        }
        return kspRoot
    }
}
