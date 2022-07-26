package com.zsu.ksp.ide.poet

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

fun KtDeclaration.resolveToTypeName(): TypeName? {
    return (resolveToDescriptorIfAny(BodyResolveMode.FULL) as? CallableDescriptor)
        ?.returnType?.asTypeName()
}

fun KotlinType.asTypeName(): TypeName {
    val rootTypeFqn = requireNotNull(fqName?.asString()) { "analyzing type: $this," }
    val rootTypeName = ClassName.bestGuess(rootTypeFqn)
    val arguments = arguments
    if (arguments.isEmpty()) return rootTypeName.copy(isMarkedNullable)
    return rootTypeName.parameterizedBy(arguments.map { it.type.asTypeName() })
}
