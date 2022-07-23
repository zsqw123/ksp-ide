package zsu.ksp.ide.sample

import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.vfs.VirtualFileManager
import com.zsu.ksp.ide.AnnotationLineMaker
import com.zsu.ksp.ide.KspAnnotationProcessor
import com.zsu.ksp.ide.sendKspNotify
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.uast.UDeclaration
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Icon

internal const val FAKE_FQN = "com.fake.FakeClass"

class FakeMarker : AnnotationLineMaker(FakeAnnotationProcessor()) {
    override fun icon(): Icon = KotlinIcons.SUSPEND_CALL
}

class FakeAnnotationProcessor : KspAnnotationProcessor {
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
                val dir = File(kspRoot, "com/zsu/sample")
                if (!dir.exists()) dir.mkdirs()
                val name = runReadAction {
                    annotated.uastAnchor?.sourcePsi?.text?.capitalizeAsciiOnly() ?: "STUB"
                }
                val aKtFile = File(dir, "Fake$name.kt")
                aKtFile.writeText(
                    "package com.zsu.sample\n" +
                        "class Fake$name",
                )
                sendKspNotify("Ksp files generate success")
                VirtualFileManager.getInstance().asyncRefresh(null)
                indicator.stop()
            } finally {
                taskIsRunning.set(false)
            }
        }
    }
}
