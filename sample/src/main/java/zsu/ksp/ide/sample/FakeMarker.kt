package zsu.ksp.ide.sample

import com.intellij.openapi.progress.runBackgroundableTask
import com.zsu.ksp.ide.AnnotationLineMaker
import com.zsu.ksp.ide.KspAnnotationProcessor
import com.zsu.ksp.ide.sendKspNotify
import org.jetbrains.uast.UDeclaration
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal const val FAKE_FQN = "com.fake.FakeClass"

class FakeMarker : AnnotationLineMaker(FakeAnnotationProcessor())

class FakeAnnotationProcessor : KspAnnotationProcessor() {
    override val annotationFqn: String = FAKE_FQN
    private val taskIsRunning = AtomicBoolean()
    override fun processAnnotated(annotated: UDeclaration, kspRoot: File) {
        val sourcePsi = annotated.sourcePsi ?: return
        runBackgroundableTask(
            "Generate KSP Files", sourcePsi.project, cancellable = false,
        ) { indicator ->
            if (taskIsRunning.get()) return@runBackgroundableTask
            try {
                taskIsRunning.set(true)
                indicator.isIndeterminate = false
                repeat(3) {
                    indicator.fraction = (it + 1) / 3.0
                    Thread.sleep(1000)
                }
                sendKspNotify("Ksp files generate success")
                indicator.stop()
            } finally {
                taskIsRunning.set(false)
            }
        }
    }
}
