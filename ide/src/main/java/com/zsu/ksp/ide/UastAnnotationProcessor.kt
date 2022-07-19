package com.zsu.ksp.ide

import org.jetbrains.uast.UDeclaration

interface UastAnnotationProcessor {
    fun processAnnotated(annotated: UDeclaration)
}
