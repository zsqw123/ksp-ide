package com.zsu.ksp.ide

import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

abstract class SimpleAnnotationProcessor<D> : KspAnnotationProcessor {
    private val taskIsRunning = AtomicBoolean()

    /**
     * read annotated [KtDeclaration], convert to our data which can be used to generate some files.
     */
    abstract fun readAnnotated(annotated: KtDeclaration): D

    /**
     * write data to file, don't do any psi related things here.
     */
    abstract fun writeToFile(data: D, kspRoot: File)
    override fun processAnnotated(annotated: KtDeclaration, kspRoot: File) {
        if (taskIsRunning.get()) return
        runBackgroundableTask(
            "Generate KSP Files", annotated.project, cancellable = false,
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
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                taskIsRunning.set(false)
            }
        }
    }
}
