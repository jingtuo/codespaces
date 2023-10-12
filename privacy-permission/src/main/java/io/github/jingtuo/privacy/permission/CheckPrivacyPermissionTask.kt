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
import org.gradle.api.tasks.OutputDirectory
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

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun execute() {
        val currentThreadName = Thread.currentThread().name
        //默认线程: Execution worker Thread 7
        println("check privacy permission start on thread : $currentThreadName")
        val startTime = System.currentTimeMillis()
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
        val mappingFilePath = getMappingFilePath().get()
        var permissionSpecs: List<PermissionSpec>?
        FileInputStream(getPermissionSpecsFile().get().asFile).use {
            permissionSpecs = Json.decodeFromStream(it)
        }
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
        val folder = getOutputDir().get().asFile
        runBlocking {
            permissionSpecs?.let {
                for ((index, item) in it.withIndex()) {
                    //使用Dispatchers.Default、Dispatchers.IO分配的线程如下:
                    //DefaultDispatcher-worker-1、DefaultDispatcher-worker-2
                    //考虑自己机器的内存不足, 此处不适用并行
                    val findStartTime = System.currentTimeMillis()
                    val referencesTo = item.getReferencesTo()
                    println("find reference($index, ${item.name}) start on thread: $currentThreadName")
                    val referenceTree =
                        getReferenceTree(command, apkFilePath, mappingFilePath, referencesTo)
                    export(folder, index, item, referenceTree)
                    println("find reference($index, ${item.name}) end cost time(${System.currentTimeMillis() - findStartTime}) on thread: $currentThreadName")
                }
            }
        }
//        permissionSpecs?.let {
//            export(it, map)
//        }
        println("check privacy permission end cost ${System.currentTimeMillis() - startTime} on thread : $currentThreadName")
    }

    /**
     * 获取所有的dex
     */
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
    private fun export(
        permissionSpecs: List<PermissionSpec>,
        map: Map<String, List<List<String>>>
    ) {
        val folder = getOutputDir().get().asFile
        if (!folder.exists()) {
            folder.mkdirs()
        }
        for ((pIndex, permission) in permissionSpecs.withIndex()) {
            //由于将所有权限写在一个文件中, 会出现Java内存不足, 所以将每个权限写一个文件
            //由于csv无法满足单元格内换行, 引入poi
            val wb = SXSSFWorkbook(100)
            val stackStyle = wb.createCellStyle()
            stackStyle.alignment = HorizontalAlignment.LEFT
            stackStyle.verticalAlignment = VerticalAlignment.CENTER
            stackStyle.wrapText = true
            stackStyle.shrinkToFit = false
            FileOutputStream(
                File(folder, permission.name + "-$pIndex.xlsx")
            ).use { stream ->
                //创建Sheet
                val sheet = wb.createSheet(permission.name)
                //创建标题
                val titleRow = sheet.createRow(0)
                val titleCell = titleRow.createCell(0, CellType.STRING)
                titleCell.setCellValue("堆栈")
                val referencesTo = permission.getReferencesTo()
                val list = map[referencesTo]
                var rowIndex = sheet.lastRowNum + 1
                //用于设置列的宽度
                var textMaxWidth = 0
                list?.let {
                    for (stack in it) {
                        var str = ""
                        for ((sIndex, line) in stack.withIndex()) {
                            if (line.length > textMaxWidth) {
                                textMaxWidth = line.length
                            }
                            str += line
                            if (sIndex != stack.size - 1) {
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
                //一个字符宽度是256, 支持的最大字符个数: 255
                var columnWidth = textMaxWidth.coerceAtMost(255) * 256
                //此处不使用autoSizeColumn, 当数据量过大时, 耗时明显增长很多
                sheet.setColumnWidth(0, columnWidth)
                wb.write(stream)
            }
            wb.dispose()
        }
    }

    private fun export(folder: File, index: Int, permissionSpec: PermissionSpec, referenceTree: List<List<String>>) {
        val wb = SXSSFWorkbook(100)
        val stackStyle = wb.createCellStyle()
        stackStyle.alignment = HorizontalAlignment.LEFT
        stackStyle.verticalAlignment = VerticalAlignment.CENTER
        stackStyle.wrapText = true
        stackStyle.shrinkToFit = false
        FileOutputStream(
            File(folder, permissionSpec.name + "-$index.xlsx")
        ).use { stream ->
            //创建Sheet
            val sheet = wb.createSheet(permissionSpec.name)
            //创建标题
            val titleRow = sheet.createRow(0)
            val titleCell = titleRow.createCell(0, CellType.STRING)
            titleCell.setCellValue("堆栈")
            var rowIndex = sheet.lastRowNum + 1
            //用于设置列的宽度
            var textMaxWidth = 0
            for (stack in referenceTree) {
                var str = ""
                for ((sIndex, line) in stack.withIndex()) {
                    if (line.length > textMaxWidth) {
                        textMaxWidth = line.length
                    }
                    str += line
                    if (sIndex != stack.size - 1) {
                        str += "\n"
                    }
                }
                val itemRow = sheet.createRow(rowIndex++)
                val stackCell = itemRow.createCell(0, CellType.STRING)
                stackCell.cellStyle = stackStyle
                stackCell.setCellValue(str)
            }
            //一个字符宽度是256, 支持的最大字符个数: 255
            val columnWidth = textMaxWidth.coerceAtMost(255) * 256
            //此处不使用autoSizeColumn, 当数据量过大时, 耗时明显增长很多
            sheet.setColumnWidth(0, columnWidth)
            wb.write(stream)
        }
        wb.dispose()
    }
}