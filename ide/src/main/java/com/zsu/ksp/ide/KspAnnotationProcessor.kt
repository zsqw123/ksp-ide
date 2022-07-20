package com.zsu.ksp.ide

import org.jetbrains.uast.UDeclaration
import java.io.File

abstract class KspAnnotationProcessor {
    abstract val annotationFqn: String
    abstract fun processAnnotated(annotated: UDeclaration, kspRoot: File)
}
