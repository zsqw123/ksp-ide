package zsu.ksp.ide.sample

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.zsu.ksp.ide.AnnotationLineMaker
import com.zsu.ksp.ide.poet.PoetAnnotationProcessor
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.psi.KtDeclaration
import javax.swing.Icon

internal const val FAKE_FQN = "org.example.Anno"

class FakeMarker : AnnotationLineMaker(FakeAnnotationProcessor()) {
    override fun icon(): Icon = KotlinIcons.SUSPEND_CALL
}

class FakeAnnotationProcessor : PoetAnnotationProcessor() {
    override val annotationFqn: String = FAKE_FQN
    override fun readAnnotated(annotated: KtDeclaration): FileSpec? {
        val pkg = annotated.containingFile.packageName ?: return null
        val className = annotated.name ?: return null
        return sampleFile(pkg, className)
    }

    private fun sampleFile(packageName: String, className: String): FileSpec {
        val fakeClassName = "Fake${className.capitalize()}"
        val simpleClass = TypeSpec.classBuilder(fakeClassName)
        simpleClass.addFunction(
            FunSpec.builder("ccc")
                .addModifiers(KModifier.PUBLIC)
                .returns(ClassName.bestGuess("$packageName.$fakeClassName"))
                .addStatement("val a = \"1\"")
                .addStatement("return Fake${className.capitalize()}()")
                .build(),
        )
        return FileSpec.builder(packageName, fakeClassName)
            .addType(simpleClass.build())
            .build()
    }
}
