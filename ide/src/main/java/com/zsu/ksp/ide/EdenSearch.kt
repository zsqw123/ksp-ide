package com.zsu.ksp.ide

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.idea.stubindex.KotlinAnnotationsIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration

// from https://github.com/zsqw123/Eden/blob/master/eden-ide/src/main/java/com/zsu/eden/EdenSearch.kt
object EdenSearch {
    private fun getAnnotation(project: Project, annotationFQN: String): PsiClass? {
        return JavaPsiFacade.getInstance(project)
            .findClass(annotationFQN, GlobalSearchScope.allScope(project))
    }

    fun getAnnotatedElements(
        project: Project, annotationFQN: String,
        scope: SearchScope = GlobalSearchScope.allScope(project)
    ): List<KtDeclaration> = search(project, annotationFQN, scope)

    // Copied some from com.android.tools.idea.dagger.DaggerAnnotatedElementsSearch
    private fun search(
        project: Project,
        annotationFQN: String,
        scope: SearchScope,
    ): List<KtDeclaration> {
        val annotationClass = getAnnotation(project, annotationFQN) ?: return emptyList()
        val candidates = getKotlinAnnotationCandidates(annotationClass, scope)
        return candidates.filterIsInstance<KtAnnotationEntry>().mapNotNull { annotation ->
            PsiTreeUtil.getParentOfType(annotation, KtDeclaration::class.java)
        }
    }

    // Copied from KotlinAnnotatedElementsSearcher#getKotlinAnnotationCandidates
    private fun getKotlinAnnotationCandidates(
        annClass: PsiClass,
        useScope: SearchScope
    ): Collection<PsiElement> {
        if (useScope is GlobalSearchScope) {
            val name = annClass.name ?: return emptyList()
            val scope = KotlinSourceFilterScope.sourcesAndLibraries(useScope, annClass.project)
            return KotlinAnnotationsIndex.getInstance().get(name, annClass.project, scope)
        }
        return (useScope as LocalSearchScope).scope.flatMap { it.descendantsOfType<KtAnnotationEntry>() }
    }
}
