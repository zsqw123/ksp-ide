package com.zsu.ksp.ide.poet

import com.squareup.kotlinpoet.FileSpec
import com.zsu.ksp.ide.SimpleAnnotationProcessor
import java.io.File

abstract class PoetAnnotationProcessor : SimpleAnnotationProcessor<FileSpec?>() {
    final override fun writeToFile(data: FileSpec?, kspRoot: File) {
        data ?: return
        if (kspRoot.exists()) {
            if (!kspRoot.isDirectory) {
                kspRoot.delete()
                kspRoot.mkdirs()
            }
        } else {
            kspRoot.mkdirs()
        }
        data.writeTo(kspRoot)
    }
}
