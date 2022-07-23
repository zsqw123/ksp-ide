package com.zsu.ksp.ide

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.configuration.externalProjectPath
import org.jetbrains.kotlin.idea.util.module
import java.io.File
import java.util.Properties
import com.intellij.openapi.module.Module as IdeaModule

fun readKspPath(element: PsiElement): File? {
    val module: IdeaModule = element.module ?: return null
    val modulePath = module.externalProjectPath ?: return null
    val moduleKspPath = readKspFromProperties(File(modulePath, "gradle.properties"))
    if (moduleKspPath != null) return File(modulePath, moduleKspPath)
    val projectPath = element.project.basePath ?: return null
    val projectKspPath = readKspFromProperties(File(projectPath, "gradle.properties"))
    if (projectKspPath != null) return File(modulePath, projectKspPath)
    return null
}

private fun readKspFromProperties(file: File): String? {
    if (!file.exists()) return null
    file.inputStream().use { fin ->
        val properties = Properties()
        properties.load(fin)
        return properties.getProperty(KSP_GENERATE_DIR)
    }
}

private const val KSP_GENERATE_DIR = "ksp_generate_dir"

fun sendKspNotify(notifyContent: String) {
    Notifications.Bus.notify(
        Notification(
            "fast.ksp.generate.ide", "",
            notifyContent, NotificationType.INFORMATION,
        ),
    )
}
