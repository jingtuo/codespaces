package io.github.jingtuo.privacy.permission

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.StringBuilder

/**
 * Kotlin默认将class标记为final class, Gradle的Task不支持final class
 */
abstract class CheckPrivacyPermissionTask : DefaultTask() {

    /**
     * cmd
     */
    @InputDirectory
    abstract fun getCmdlineToolsDir(): DirectoryProperty

    /**
     * apk文件
     */
    @InputFile
    abstract fun getApkFile(): RegularFileProperty

    /**
     * mapping文件
     */
    @Input
    abstract fun getMappingFilePath(): Property<String>

    /**
     * 权限描述文件, Json格式
     */
    @InputFile
    abstract fun getPermissionSpecsFile(): RegularFileProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun execute() {
        val cmdlineToolsHome = getCmdlineToolsDir().get().asFile.path
        //Windows 11
        val osName = System.getProperty("os.name")
        val isWindow = osName.contains("Windows", true);
        val commandName = if (isWindow) {
            "apkanalyzer.bat"
        } else {
            "apkanalyzer.sh"
        }
        //environment对应于系统的环境变量
        val path = System.getenv("path")
        val pathArray = if (isWindow) {
            path?.split(";")
        } else {
            path?.split(":")
        } ?: emptyList()
        //检测环境变量中是否配置了apkanalyzer
        var exists = false
        for (item in pathArray) {
            val file = File(item)
            val children = file.listFiles { _, name ->
                commandName.equals(name, false)
            }
            if (children != null && children.isNotEmpty()) {
                exists = true
                break;
            }
        }
        val command = if (exists) {
            commandName
        } else {
            "${cmdlineToolsHome}${File.separator}bin${File.separator}$commandName"
        }
        println("command: $command")
        //将错误输出流合并到标准输出流
        val apkFilePath = getApkFile().get().asFile.path
        val allDex = getAllDex(command, apkFilePath)
        for (item in allDex) {
            println("dex: $item")
        }
        val mappingFilePath = getMappingFilePath().get()
        val permissionSpecs: List<PermissionSpec> = Json.decodeFromStream(
            FileInputStream(getPermissionSpecsFile().get().asFile)
        )
        permissionSpecs.map {
            var matchContent = "L${it.clsName.replace("\\.", "/")};->"
            matchContent += if (it.isField) {
                "${it.fieldName}:${getDexType(it.fieldType)}"
            } else {
                "${it.methodName}()L${getDexType(it.methodReturnType)}"
            }
            println(matchContent)
            matchContent
        }

        val referencesTos = permissionSpecs.map {
            if (it.isField) {
                it.clsName + " " + it.fieldType + " " + it.fieldName
            } else {
                it.clsName + " " + it.methodReturnType + " " + it.methodName + "()"
            }
        }

        for (referencesTo in referencesTos) {
            val referenceTree = getReferenceTree(command, apkFilePath, mappingFilePath, referencesTo)
            println(referencesTo)
            println("---start---")
            println(referenceTree)
            println("---end---")
        }

        println("task apk file: ${getApkFile().get().asFile.path}")
        println("buildDir: ${project.buildDir}")
    }

    private fun getAllDex(command: String, apkFilePath: String): List<String> {
        val process = ProcessBuilder(command, "dex", "list", apkFilePath)
            .inheritIO()
            .redirectErrorStream(true)
            .start()
        val result = mutableListOf<String>()
        BufferedReader(InputStreamReader(process.inputStream)).use {
            while (true) {
                val line = it.readLine() ?: break
                result.add(line)
            }
        }
        process.destroy()
        return result.toList()
    }

    /***
     * 格式:
     * 类:
     * C d 10	10	2015	com.hundsun.main.WinnerApplication
     * 属性:
     * F d 0	0	10	com.hundsun.main.WinnerApplication java.lang.String TAG
     * F d 0	0	12	com.hundsun.main.WinnerApplication boolean hasInit
     * 方法:
     * M d 1	1	44	com.hundsun.main.WinnerApplication <init>()
     * M d 1	1	231	com.hundsun.main.WinnerApplication boolean isMainProcess()
     *
     */
    private fun getDexPackages(
        command: String,
        apkFilePath: String,
        mappingFilePath: String
    ): List<String> {
        val process = ProcessBuilder(
            command, "dex", "packages", "--defined-only",
            "--proguard-mappings", mappingFilePath, apkFilePath
        )
            .inheritIO()
            .redirectErrorStream(true)
            .start()
        val result = mutableListOf<String>()
        BufferedReader(InputStreamReader(process.inputStream)).use {
            while (true) {
                val line = it.readLine() ?: break
                result.add(line)
            }
        }
        process.destroy()
        return result.toList()
    }

    private fun getClass(
        command: String,
        apkFilePath: String,
        mappingFilePath: String,
        clsName: String
    ): String {
        val process = ProcessBuilder(
            command, "dex", "code", "--class", clsName,
            "--proguard-mappings", mappingFilePath, apkFilePath
        )
            .inheritIO()
            .redirectErrorStream(true)
            .start()
        val result = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use {
            while (true) {
                val line = it.readLine() ?: break
                result.append(line).append("\n")
            }
        }
        process.destroy()
        return result.toString()
    }

    private fun getDexType(javaType: String): String = when (javaType) {
        "int", "Int" -> "I"
        "boolean", "Boolean" -> "Z"
        "void", "Unit" -> "V"
        else -> javaType.replace("\\.", "/")
    }

    private fun getReferenceTree(
        command: String, apkFilePath: String, mappingFilePath: String,
        referencesTo: String
    ): String {
        val process = ProcessBuilder(
            command, "dex", "reference-tree", "--references-to", referencesTo,
            "--proguard-mappings", mappingFilePath, apkFilePath
        )
            .inheritIO()
            .redirectErrorStream(true)
            .start()
        val result = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use {
            while (true) {
                val line = it.readLine() ?: break
                result.append(line).append("\n")
            }
        }
        process.destroy()
        return result.toString()
    }
}