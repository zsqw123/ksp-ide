package zsu.ksp.ide.sample

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.getPackage

val PsiFile.packageName: String?
    get() = containingDirectory?.getPackage()?.qualifiedName
