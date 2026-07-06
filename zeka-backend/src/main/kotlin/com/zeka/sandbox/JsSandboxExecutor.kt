package com.zeka.sandbox

import java.io.File

object JsSandboxExecutor {

    fun executeJsCode(
        workspaceId: String,
        hostWorkspacePath: String,
        jsCode: String,
        arguments: List<String> = emptyList()
    ): DockerSandboxManager.CommandResult {
        // Ensure host workspace path exists
        val workspaceDir = File(hostWorkspacePath)
        if (!workspaceDir.exists()) {
            return DockerSandboxManager.CommandResult(-1, "", "Workspace directory does not exist.")
        }

        // Write JS code to a temporary script inside the workspace
        val tempFile = File(workspaceDir, "temp_skill_exec_${System.currentTimeMillis()}.js")
        try {
            tempFile.writeText(jsCode, Charsets.UTF_8)
            
            // Format command arguments
            val argsStr = arguments.joinToString(" ") { "\"$it\"" }
            val containerCmd = "node ${tempFile.name} $argsStr"
            
            val result = DockerSandboxManager.executeCommand(workspaceId, containerCmd)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return DockerSandboxManager.CommandResult(-1, "", "Execution failed: ${e.localizedMessage}")
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
