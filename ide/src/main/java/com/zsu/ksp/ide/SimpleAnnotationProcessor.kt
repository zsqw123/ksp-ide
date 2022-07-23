package com.zsu.ksp.ide

import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.uast.UDeclaration
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

abstract class SimpleAnnotationProcessor<D> : KspAnnotationProcessor {
    private val taskIsRunning = AtomicBoolean()
    abstract fun readAnnotated(annotated: UDeclaration): D
    abstract fun writeToFile(data: D, kspRoot: File)
    override fun processAnnotated(annotated: UDeclaration, kspRoot: File) {
        if (taskIsRunning.get()) return
        val sourcePsi = annotated.sourcePsi ?: return
        runBackgroundableTask(
            "Generate KSP Files", sourcePsi.project, cancellable = false,
        ) { indicator ->
            try {
                taskIsRunning.set(true)
                indicator.isIndeterminate = false
                val data = runReadAction {
                    readAnnotated(annotated)
                }
                indicator.fraction = 0.5
                writeToFile(data, kspRoot)
                sendKspNotify("Ksp files generate success")
                VirtualFileManager.getInstance().asyncRefresh(null)
                indicator.fraction = 1.0
                indicator.stop()
            } finally {
                taskIsRunning.set(false)
            }
        }
    }
}
