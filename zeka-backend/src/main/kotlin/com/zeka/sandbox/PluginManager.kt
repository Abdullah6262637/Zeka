package com.zeka.sandbox

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val entryPoint: String? = null
)

@Serializable
data class ZekaPlugin(
    val manifest: PluginManifest,
    val installedPath: String,
    var isActive: Boolean = true
)

object PluginManager {

    private val plugins = mutableMapOf<String, ZekaPlugin>()
    private val json = Json { ignoreUnknownKeys = true }

    fun installPlugin(zipStream: java.io.InputStream, workspacePath: String): ZekaPlugin? {
        val pluginsDir = File(workspacePath, ".agents/plugins")
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
        }

        val tempDir = File(pluginsDir, "temp_install_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val zis = ZipInputStream(zipStream)
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(tempDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }

            // Find manifest.json
            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                tempDir.deleteRecursively()
                return null
            }

            val manifestContent = manifestFile.readText(Charsets.UTF_8)
            val manifest = json.decodeFromString<PluginManifest>(manifestContent)

            val targetDir = File(pluginsDir, manifest.id)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            tempDir.renameTo(targetDir)

            val plugin = ZekaPlugin(
                manifest = manifest,
                installedPath = targetDir.absolutePath,
                isActive = true
            )
            plugins[manifest.id] = plugin
            return plugin
        } catch (e: Exception) {
            e.printStackTrace()
            tempDir.deleteRecursively()
            return null
        }
    }

    fun listPlugins(): List<ZekaPlugin> = plugins.values.toList()

    fun togglePlugin(pluginId: String, active: Boolean): Boolean {
        val p = plugins[pluginId] ?: return false
        p.isActive = active
        return true
    }
}
