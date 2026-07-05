package com.zeka.sandbox

import java.io.File

object PromptSkillLoader {

    fun loadSkillsFromWorkspace(workspacePath: String): String {
        val skillsDir = File(workspacePath, ".agents/skills")
        if (!skillsDir.exists() || !skillsDir.isDirectory) {
            return ""
        }

        val sb = java.lang.StringBuilder()
        sb.append("\n\n=== MEVCUT KULLANICI YETENEKLERİ (SKILLS) ===\n")
        sb.append("İhtiyacın halinde aşağıdaki özel yeteneklerden ve talimatlardan faydalanabilirsin:\n\n")

        val skillFolders = skillsDir.listFiles { file -> file.isDirectory } ?: emptyArray()
        for (folder in skillFolders) {
            val skillFile = File(folder, "SKILL.md")
            if (skillFile.exists()) {
                val content = skillFile.readText(Charsets.UTF_8)
                val body = extractSkillBody(content)
                sb.append("Yetenek Adı: ${folder.name}\n")
                sb.append("Talimatlar:\n$body\n")
                sb.append("--------------------------------------------------\n")
            }
        }
        return sb.toString()
    }

    private fun extractSkillBody(content: String): String {
        if (content.startsWith("---")) {
            val parts = content.split("---", limit = 3)
            if (parts.size >= 3) {
                return parts[2].trim()
            }
        }
        return content.trim()
    }
}
