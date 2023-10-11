package io.github.jingtuo.privacy.permission

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.streaming.SXSSFCell
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
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
    @Input
    abstract fun getApkFilePath(): Property<String>

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

    @OutputFile
    abstract fun getOutputFile(): RegularFileProperty

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
        //将错误输出流合并到标准输出流
        val apkFilePath = getApkFilePath().get()
        val allDex = getAllDex(command, apkFilePath)
        for (item in allDex) {
            println("dex: $item")
        }
        val mappingFilePath = getMappingFilePath().get()
        var permissionSpecs: List<PermissionSpec>?
        FileInputStream(getPermissionSpecsFile().get().asFile).use {
            permissionSpecs = Json.decodeFromStream(it)
        }
        println("convert permissionSpecs to dex code: ")
        permissionSpecs?.map {
            var matchContent = "L${it.clsName.replace("\\.", "/")};->"
            matchContent += if (it.isField) {
                "${it.fieldName}:${getDexType(it.fieldType)}"
            } else {
                "${it.methodName}()L${getDexType(it.methodReturnType)}"
            }
            println(matchContent)
            matchContent
        }

        val threadName = Thread.currentThread().name
        val map = mutableMapOf<String, List<List<String>>>()
        runBlocking {
            permissionSpecs?.let {
                for (item in it) {
                    launch(Dispatchers.IO) {
                        val subTreadName = Thread.currentThread().name
                        val referencesTo = item.getReferencesTo()
                        val startTime = System.currentTimeMillis()
                        println("find reference($referencesTo) start on thread: $subTreadName")
                        val referenceTree =
                            getReferenceTree(command, apkFilePath, mappingFilePath, referencesTo)
                        map[referencesTo] = referenceTree
                        println("find reference($referencesTo) end cost time(${System.currentTimeMillis() - startTime}) on thread: $subTreadName")
                    }
                }
            }
        }
        permissionSpecs?.let {
            export(it, map)
        }
        println("complete on thread: $threadName")
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

    /**
     * 示例:
     * android.app.ActivityManager java.util.List getRunningAppProcesses()
     *  androidx.work.impl.utils.ProcessUtils java.lang.String getProcessName(android.content.Context)
     *   androidx.work.impl.utils.ProcessUtils boolean isDefaultProcess(android.content.Context,androidx.work.Configuration)
     *    androidx.work.impl.background.greedy.GreedyScheduler void checkDefaultProcess()
     *     androidx.work.impl.background.greedy.GreedyScheduler void cancel(java.lang.String)
     *     androidx.work.impl.background.greedy.GreedyScheduler void schedule(androidx.work.impl.model.WorkSpec[])
     *      androidx.work.impl.background.greedy.DelayedWorkTracker$1 void run()
     *    androidx.work.impl.utils.ForceStopRunnable boolean multiProcessChecks()
     *     androidx.work.impl.utils.ForceStopRunnable void run()
     */
    private fun getReferenceTree(
        command: String, apkFilePath: String, mappingFilePath: String,
        referencesTo: String
    ): List<List<String>> {
        val process = ProcessBuilder(
            command, "dex", "reference-tree", "--references-to", referencesTo,
            "--proguard-mappings", mappingFilePath, apkFilePath
        )
            .inheritIO()
            .start()
        val result = mutableListOf<List<String>>()
        var curStack = mutableListOf<String>()
        var curCount = 0
        BufferedReader(InputStreamReader(process.inputStream)).use {
            while (true) {
                val line = it.readLine()
                if (line == null) {
                    result.add(curStack)
                    break
                }
                val count = line.countStartsWith(' ')
                if (0 == count) {
                    //首行
                    curStack = mutableListOf()
                    curStack.add(line)
                } else {
                    if (count <= curCount) {
                        //新的引用入口, 保存之前的堆栈
                        result.add(curStack)
                        //取集合的第0个索引到第endIndex个元素(包含endIndex)
                        val endIndex = count - 1
                        curStack = curStack.slice(0..endIndex).toMutableList()
                    }
                    curStack.add(line.trimStart())
                }
                curCount = count
            }
        }
        var error = ""
        BufferedReader(InputStreamReader(process.errorStream)).use {
            while (true) {
                val line = it.readLine() ?: break
                error += "$line\n"
            }
        }
        if (!error.isNullOrEmpty()) {
            println("$referencesTo error:\n$error ")
        }
        process.destroy()
        return result
    }

    /**
     * 导出数据
     */
    private fun export(permissionSpecs: List<PermissionSpec>, map: Map<String, List<List<String>>>) {
        //由于csv无法满足单元格内换行, 引入poi
        val wb = SXSSFWorkbook(100)
        val stackStyle = wb.createCellStyle()
        stackStyle.alignment = HorizontalAlignment.LEFT
        stackStyle.verticalAlignment = VerticalAlignment.CENTER
        stackStyle.wrapText = true
        stackStyle.shrinkToFit = false
        FileOutputStream(getOutputFile().get().asFile).use { stream ->
            permissionSpecs?.let { permissions ->
                for (permission in permissions) {
                    //创建Sheet, 考虑一个sheet的行数有上限, 按照权限名分在多个sheet中
                    var sheet = wb.getSheet(permission.name)
                    if (sheet == null) {
                        sheet = wb.createSheet(permission.name)
                        //创建标题
                        val titleRow = sheet.createRow(0)
                        val titleCell = titleRow.createCell(0, CellType.STRING)
                        titleCell.setCellValue("堆栈")
                    }
                    println("sheet: ${sheet.sheetName}, ${sheet.lastRowNum}")
                    val referencesTo = permission.getReferencesTo()
                    val list = map[referencesTo]
                    var rowIndex = sheet.lastRowNum + 1
                    //用于设置列的宽度
                    var textMaxWidth = 0
                    list?.let {
                        for (stack in it) {
                            var str = ""
                            for ((index, line) in stack.withIndex()) {
                                if (line.length > textMaxWidth) {
                                    textMaxWidth = line.length
                                }
                                str += line
                                if (index != stack.size - 1) {
                                    str += "\n"
                                }
                            }
                            val itemRow = sheet.createRow(rowIndex++)
//                            itemRow.height = (stack.size * 20).toShort()
                            val stackCell = itemRow.createCell(0, CellType.STRING)
                            stackCell.cellStyle = stackStyle
                            stackCell.setCellValue(str)
                        }
                    }
                    var columnWidth = sheet.getColumnWidth(0)
                    //一个字符宽度是256
                    columnWidth = columnWidth.coerceAtLeast(textMaxWidth * 256)
                    //支持的最大字符个数: 255
                    columnWidth = columnWidth.coerceAtMost(255 * 256)
                    println("column width: ${columnWidth / 256}")
                    //此处不使用autoSizeColumn, 当数据量过大时, 耗时明显增长很多
                    sheet.setColumnWidth(0, columnWidth)
                }
            }
            wb.write(stream)
        }
        wb.dispose()
    }
}