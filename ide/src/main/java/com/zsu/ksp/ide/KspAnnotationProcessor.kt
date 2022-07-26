package com.zsu.ksp.ide

import org.jetbrains.kotlin.psi.KtDeclaration
import java.io.File

interface KspAnnotationProcessor {
    val annotationFqn: String
    fun requireProcessAllAnnotated(annotatedContext: KtDeclaration): Boolean = false
    fun processAnnotated(annotated: KtDeclaration, kspRoot: File)
    fun processAllAnnotated(allAnnotated: List<KtDeclaration>, kspRoot: File) {
        for (annotated in allAnnotated) {
            processAnnotated(annotated, kspRoot)
        }
    }
}
