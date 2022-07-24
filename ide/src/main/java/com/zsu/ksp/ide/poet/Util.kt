package com.zsu.ksp.ide.poet

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UElement
import kotlin.jvm.internal.Reflection

val PsiFile.packageName: String?
    get() = containingDirectory?.getPackage()?.qualifiedName

/**
 * PsiType parsing with wildcard often produces strange results, which doesn't help us
 * convert it to [TypeName].
 * such as: java.util.List<? extends A> will be converted to java.util.List<A>
 */
val PsiType.fqnIgnoreWildcard: String
    get() {
        if (this is PsiPrimitiveType) return requireNotNull(boxedTypeName)
        if (this is PsiClassType) {
            if (parameterCount == 0) return canonicalText
            else if (this is PsiClassReferenceType) {
                val ref = reference
                val qualifier = ref.qualifiedName?.substringBefore("<")
                    ?: return ref.canonicalText
                val params = ref.typeParameters
                if (params.isEmpty()) return ref.canonicalText
                val paramsText = params.joinToString(
                    prefix = "<", postfix = ">",
                ) { it.fqnIgnoreWildcard }
                return qualifier + paramsText
            }
        }
        if (this is PsiWildcardType) {
            return bound?.fqnIgnoreWildcard ?: canonicalText
        }
        return canonicalText
    }

val PsiType.firstTypeParameter: PsiType?
    get() {
        if (this !is PsiClassType) return null
        return parameters.firstOrNull()
    }

/**
 * convert java [PsiType] to [TypeName], it will analyze all typeParameters to construct the
 * right [TypeName].
 * @return kotlin poet TypeName
 */
fun PsiType.asTypeName(nullable: Boolean? = null, ignoreWildcard: Boolean = true): TypeName {
    if (this is PsiWildcardType) {
        val typeName = bound?.asTypeName() ?: ClassName.bestGuess(fqnIgnoreWildcard)
        if (ignoreWildcard) return typeName
        if (isSuper) return WildcardTypeName.consumerOf(typeName)
        if (isExtends) return WildcardTypeName.producerOf(typeName)
        return typeName
    }
    if (this !is PsiClassType) {
        return ClassName.bestGuess(fqnIgnoreWildcard.java2KtQualifiedName())
    }
    val params = parameters
    if (params.isEmpty()) {
        return ClassName.bestGuess(fqnIgnoreWildcard.java2KtQualifiedName())
    }
    val rootFqn = fqnIgnoreWildcard.substringBefore('<')
    val className = ClassName.bestGuess(rootFqn.java2KtQualifiedName())
    val typeName = className.parameterizedBy(
        params.map { it.asTypeName(ignoreWildcard = ignoreWildcard) },
    )
    if (nullable != null) return typeName.copy(nullable = nullable)
    return typeName
}


/**
 * convert java type fqn to kotlin type fqn, such as: [java.util.List] to [kotlin.collections.List]
 * @receiver java type fqn
 * @return kotlin type fqn
 */
fun String.java2KtQualifiedName(): String {
    runCatching {
        val clazz = Class.forName(this)
        val klazz = Reflection.getOrCreateKotlinClass(clazz)
        return klazz.qualifiedName ?: this
    }
    return this
}


/**
 * In general, the kotlin nullable [KtElement] is converted to [UElement] with the @[Nullable]
 * annotation, which is used here to determine whether it is null or not.
 * @return return nullability, null if it cannot infer nullability.
 */
fun UAnnotated.nullable(): Boolean? {
    val annotations = uAnnotations
    for (annotation in annotations) {
        val qName = annotation.qualifiedName ?: continue
        if (qName.endsWith("Nullable")) return true
        else if (qName.endsWith("NotNull")) return false
    }
    return null
}
