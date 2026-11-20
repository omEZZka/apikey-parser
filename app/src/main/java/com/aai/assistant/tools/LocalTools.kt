package com.aai.assistant.tools

import com.aai.assistant.domain.AgentTool
import com.aai.assistant.domain.ToolResult
import com.aai.assistant.domain.ToolSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.util.concurrent.TimeUnit

class RunShellTool : AgentTool {
    override val name = "run_shell"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Выполняет shell-команду на устройстве (без root). Возвращает stdout, stderr, exitCode. Таймаут по умолчанию 30 сек.",
        ToolSchemas.params(
            "command" to ToolSchemas.stringParam("Команда для /system/bin/sh -c"),
            "timeout_ms" to ToolSchemas.intParam("Таймаут в мс (по умолчанию 30000, максимум 120000)"),
            "workdir" to ToolSchemas.stringParam("Рабочая директория (необязательно)"),
            required = listOf("command")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val command = arguments["command"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента command")
        val timeout = (arguments["timeout_ms"]?.jsonPrimitive?.longOrNull ?: 30_000L).coerceIn(1_000, 120_000)
        val workdir = arguments["workdir"]?.jsonPrimitive?.contentOrNull
        try {
            val builder = ProcessBuilder("/system/bin/sh", "-c", command)
            workdir?.let {
                val dir = File(it)
                if (dir.isDirectory) builder.directory(dir)
            }
            val process = builder.start()
            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val outThread = Thread {
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line -> synchronized(stdout) { stdout.appendLine(line) } }
                }
            }
            val errThread = Thread {
                process.errorStream.bufferedReader().use { reader ->
                    reader.forEachLine { line -> synchronized(stderr) { stderr.appendLine(line) } }
                }
            }
            outThread.start()
            errThread.start()
            val finished = process.waitFor(timeout, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                outThread.join(500)
                errThread.join(500)
                return@withContext ToolResult(
                    buildJsonObject {
                        put("exitCode", -1)
                        put("timeout", true)
                        put("stdout", stdout.toString().take(20_000))
                        put("stderr", "Timeout after ${timeout}ms. " + stderr.toString().take(5_000))
                    }.toString(),
                    isError = true
                )
            }
            outThread.join(2_000)
            errThread.join(2_000)
            val result = buildJsonObject {
                put("exitCode", process.exitValue())
                put("stdout", stdout.toString().take(20_000))
                put("stderr", stderr.toString().take(8_000))
            }
            ToolResult(result.toString(), isError = process.exitValue() != 0)
        } catch (e: Exception) {
            ToolResult.error("Ошибка запуска: ${e.message}")
        }
    }
}

abstract class FileTool(override val name: String) : AgentTool {
    protected fun resolve(path: String): File =
        if (path.startsWith("/")) File(path)
        else File(android.os.Environment.getExternalStorageDirectory(), path)
}

class FileReadTool : FileTool("file_read") {
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Читает текстовый файл. Относительные пути считаются от /sdcard. Максимум 60000 символов.",
        ToolSchemas.params(
            "path" to ToolSchemas.stringParam("Путь к файлу"),
            required = listOf("path")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = arguments["path"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента path")
        val file = resolve(path)
        when {
            !file.exists() -> ToolResult.error("Файл не существует: ${file.absolutePath}")
            !file.isFile -> ToolResult.error("Это не файл: ${file.absolutePath}")
            file.length() > 5_000_000 -> ToolResult.error("Файл слишком большой (>5MB): ${file.length()} байт")
            else -> try {
                val text = file.readText()
                ToolResult(if (text.length > 60_000) text.take(60_000) + "\n...[обрезано]" else text)
            } catch (e: Exception) {
                ToolResult.error("Ошибка чтения: ${e.message}")
            }
        }
    }
}

class FileWriteTool : FileTool("file_write") {
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Записывает текст в файл (создаёт директории, перезаписывает существующий). Относительные пути от /sdcard.",
        ToolSchemas.params(
            "path" to ToolSchemas.stringParam("Путь к файлу"),
            "content" to ToolSchemas.stringParam("Содержимое"),
            "append" to ToolSchemas.stringParam("'true' чтобы дописать в конец (необязательно)"),
            required = listOf("path", "content")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = arguments["path"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента path")
        val content = arguments["content"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента content")
        val append = arguments["append"]?.jsonPrimitive?.contentOrNull == "true"
        val file = resolve(path)
        try {
            file.parentFile?.mkdirs()
            if (append) file.appendText(content) else file.writeText(content)
            ToolResult("Записано ${content.length} символов в ${file.absolutePath}")
        } catch (e: Exception) {
            ToolResult.error("Ошибка записи: ${e.message}. Возможно нет доступа к файлам — нужен MANAGE_EXTERNAL_STORAGE.")
        }
    }
}

class FileListTool : FileTool("file_list") {
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Список файлов и папок в директории. Относительные пути от /sdcard.",
        ToolSchemas.params(
            "dir_path" to ToolSchemas.stringParam("Путь к директории"),
            required = listOf("dir_path")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = arguments["dir_path"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента dir_path")
        val dir = resolve(path)
        if (!dir.isDirectory) return@withContext ToolResult.error("Не директория: ${dir.absolutePath}")
        val entries = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: return@withContext ToolResult.error("Нет доступа к ${dir.absolutePath}")
        val text = entries.take(500).joinToString("\n") { f ->
            val type = if (f.isDirectory) "dir " else "file"
            val size = if (f.isFile) " ${f.length()}B" else ""
            "$type ${f.name}$size"
        }
        ToolResult("${dir.absolutePath} (${entries.size} записей):\n$text")
    }
}

class FileDeleteTool : FileTool("file_delete") {
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Удаляет файл или пустую директорию. Относительные пути от /sdcard.",
        ToolSchemas.params(
            "path" to ToolSchemas.stringParam("Путь"),
            required = listOf("path")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = arguments["path"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента path")
        val file = resolve(path)
        when {
            !file.exists() -> ToolResult.error("Не существует: ${file.absolutePath}")
            file.isDirectory && (file.list()?.isNotEmpty() == true) ->
                ToolResult.error("Директория не пуста, удаление запрещено: ${file.absolutePath}")
            file.delete() -> ToolResult("Удалено: ${file.absolutePath}")
            else -> ToolResult.error("Не удалось удалить: ${file.absolutePath}")
        }
    }
}