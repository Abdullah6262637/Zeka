package com.zeka.sandbox

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object DockerSandboxManager {

    data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    fun createSandbox(workspaceId: String, hostPath: String): Boolean {
        val containerName = "zeka-sandbox-$workspaceId"
        try {
            val checkProcess = ProcessBuilder("docker", "ps", "-a", "--filter", "name=$containerName", "--format", "{{.Names}}").start()
            val output = checkProcess.inputStream.bufferedReader().readText().trim()
            if (output.contains(containerName)) {
                ProcessBuilder("docker", "start", containerName).start().waitFor()
                return true
            }

            val process = ProcessBuilder(
                "docker", "run", "-d",
                "--name", containerName,
                "-v", "$hostPath:/workspace",
                "-w", "/workspace",
                "node:18-alpine",
                "tail", "-f", "/dev/null"
            ).start()
            val exitCode = process.waitFor()
            return exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun executeCommand(workspaceId: String, command: String): CommandResult {
        val containerName = "zeka-sandbox-$workspaceId"
        try {
            val process = ProcessBuilder(
                "docker", "exec", containerName,
                "sh", "-c", command
            ).start()

            val stdoutSB = StringBuilder()
            val stderrSB = StringBuilder()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) {
                stdoutSB.append(line).append("\n")
            }
            while (stderrReader.readLine().also { line = it } != null) {
                stderrSB.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            return CommandResult(exitCode, stdoutSB.toString(), stderrSB.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            return CommandResult(-1, "", e.localizedMessage ?: "Exception occurred during execution")
        }
    }

    fun stopSandbox(workspaceId: String): Boolean {
        val containerName = "zeka-sandbox-$workspaceId"
        try {
            ProcessBuilder("docker", "stop", containerName).start().waitFor()
            val process = ProcessBuilder("docker", "rm", containerName).start().waitFor()
            return process == 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
