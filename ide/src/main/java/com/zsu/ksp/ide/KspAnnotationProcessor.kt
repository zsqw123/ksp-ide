package com.zsu.ksp.ide

import org.jetbrains.uast.UDeclaration
import java.io.File

interface KspAnnotationProcessor {
    val annotationFqn: String
    fun requireProcessAllAnnotated(annotatedContext: UDeclaration): Boolean = false
    fun processAnnotated(annotated: UDeclaration, kspRoot: File)
    fun processAllAnnotated(allAnnotated: Sequence<UDeclaration>, kspRoot: File) {
        for (annotated in allAnnotated) {
            processAnnotated(annotated, kspRoot)
        }
    }
}
