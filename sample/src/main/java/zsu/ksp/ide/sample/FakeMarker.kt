package zsu.ksp.ide.sample

import com.intellij.openapi.progress.runBackgroundableTask
import com.zsu.ksp.ide.AnnotationLineMaker
import com.zsu.ksp.ide.KspAnnotationProcessor
import org.jetbrains.uast.UDeclaration
import java.io.File

internal const val FAKE_FQN = "com.fake.FakeClass"

class FakeMarker : AnnotationLineMaker(FakeAnnotationProcessor())

class FakeAnnotationProcessor : KspAnnotationProcessor() {
    override val annotationFqn: String = FAKE_FQN
    override fun processAnnotated(annotated: UDeclaration, kspRoot: File) {
        val sourcePsi = annotated.sourcePsi ?: return
        runBackgroundableTask("Generate KSP Files", sourcePsi.project) { indicator ->
            indicator.isIndeterminate = false
            repeat(3) {
                indicator.fraction = (it + 1) / 3.0
                Thread.sleep(1000)
            }
            indicator.stop()
        }
    }
}
