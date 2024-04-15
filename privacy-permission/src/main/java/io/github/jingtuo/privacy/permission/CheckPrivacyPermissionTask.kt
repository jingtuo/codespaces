package io.github.jingtuo.privacy.permission

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
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

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty

    @TaskAction
    fun execute() {
        val currentThreadName = Thread.currentThread().name
        //默认线程: Execution worker Thread 7
        val startTime = System.currentTimeMillis()
        val cmdlineToolsHome = getCmdlineToolsDir().get().asFile.path
        //Windows 11
        val osName = System.getProperty("os.name")
        println("check privacy permission start on os($osName) + main thread($currentThreadName)")
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
        val apkFilePath = getApkFile().get().asFile.absolutePath
        println("command: $command, apk path: $apkFilePath")
        val mappingFilePath = getMappingFilePath().get()
        var permissionSpecs: List<PermissionSpec>?
        val gson = Gson()
        gson.newJsonReader(FileReader(getPermissionSpecsFile().get().asFile)).use {
            permissionSpecs = gson.fromJson(it, object : TypeToken<List<PermissionSpec>>() {
            }.type)
        }
        val folder = getOutputDir().get().asFile
        permissionSpecs?.let {
            for ((index, item) in it.withIndex()) {
                //使用Dispatchers.IO分配的线程数不受限制
                //暂时未搞明白, 怎么在Gradle中用协程, 暂时用单线程
                //考虑自己机器的内存不足, 此处限定4个线程并发
                val findStartTime = System.currentTimeMillis()
                val referencesTo = item.getReferencesTo()
                println("find reference($index, ${item.name}, $referencesTo) start on thread: $currentThreadName")
                val referenceTree =
                    getReferenceTree(command, apkFilePath, mappingFilePath, referencesTo)
                export(folder, index, item, referenceTree)
                println(
                    "find reference($index, ${item.name}) end " +
                            "cost time(${System.currentTimeMillis() - findStartTime}) " +
                            "on thread: $currentThreadName"
                )
            }
        }
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
        val builder = if (mappingFilePath.isNullOrEmpty()) {
            ProcessBuilder(
                command, "dex", "packages", "--defined-only",
                apkFilePath
            )
        } else {
            ProcessBuilder(
                command, "dex", "packages", "--defined-only",
                "--proguard-mappings", mappingFilePath,
                apkFilePath
            )
        }
        val process = builder
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
        val builder = if (mappingFilePath.isNullOrEmpty()) {
            ProcessBuilder(
                command, "dex", "code", "--class", clsName,
                apkFilePath
            )
        } else {
            ProcessBuilder(
                command, "dex", "code", "--class", clsName,
                "--proguard-mappings", mappingFilePath,
                apkFilePath
            )
        }
        val process = builder
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

    private fun getDexType(javaType: String?): String = when (javaType) {
        "int", "Int" -> "I"
        "boolean", "Boolean" -> "Z"
        null, "void", "Unit" -> "V"
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
        val builder = if (mappingFilePath.isEmpty()) {
            ProcessBuilder(
                command, "dex", "reference-tree", "--references-to", referencesTo,
                apkFilePath
            )
        } else {
            ProcessBuilder(
                command, "dex", "reference-tree", "--references-to", referencesTo,
                "--proguard-mappings", mappingFilePath,
                apkFilePath
            )
        }
        val process = builder
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
        if (error.isNotEmpty()) {
            println("$referencesTo error:\n$error ")
        }
        process.destroy()
        return result
    }

    private fun export(
        folder: File,
        index: Int,
        permissionSpec: PermissionSpec,
        referenceTree: List<List<String>>
    ) {
        if (referenceTree.isEmpty()) {
            return
        }
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
            //第一列
            var titleCell = titleRow.createCell(0, CellType.STRING)
            titleCell.setCellValue("类型")
            //第二列
            titleCell = titleRow.createCell(1, CellType.STRING)
            titleCell.setCellValue("库名")
            //第三列
            titleCell = titleRow.createCell(2, CellType.STRING)
            titleCell.setCellValue("堆栈")
            var rowIndex = sheet.lastRowNum + 1
            //用于设置列的宽度
            var firstWidth = 2
            var secondWidth = 2
            var thirdWidth = 2
            for (stack in referenceTree) {
                var str = ""
                var lastLine = ""
                for ((sIndex, line) in stack.withIndex()) {
                    if (line.length > thirdWidth) {
                        thirdWidth = line.length
                    }
                    str += line
                    if (sIndex != stack.size - 1) {
                        //不是最后一个
                        str += "\n"
                    } else {
                        //最后一个
                        lastLine = line
                    }
                }
                if (str.isNullOrEmpty()) {
                    continue
                }
                val itemRow = sheet.createRow(rowIndex++)
                val (first, second) = getTypeAndLibraryName(lastLine)
                //第一列
                val firstCell = itemRow.createCell(0, CellType.STRING)
                firstCell.setCellValue(first)
                if (first.length > firstWidth) {
                    firstWidth = first.length
                }
                //第二列
                val secondCell = itemRow.createCell(1, CellType.STRING)
                secondCell.setCellValue(second)
                if (second.length > secondWidth) {
                    secondWidth = second.length
                }
                //第三列
                val thirdCell = itemRow.createCell(2, CellType.STRING)
                thirdCell.cellStyle = stackStyle
                thirdCell.setCellValue(str)
            }
            //一个字符宽度是256, 支持的最大字符个数: 255
            //此处不使用autoSizeColumn, 当数据量过大时, 耗时明显增长很多
            //第一列
            sheet.setColumnWidth(0, firstWidth.coerceAtLeast(4) * 256)
            //第二列
            sheet.setColumnWidth(1, secondWidth.coerceAtLeast(4) * 256)
            thirdWidth = thirdWidth.coerceAtMost(100) * 256
            sheet.setColumnWidth(2, thirdWidth)
            wb.write(stream)
        }
        wb.dispose()
    }

    private fun getTypeAndLibraryName(line: String): Pair<String, String> {
        if (line.startsWith("androidx.fragment.app")) {
            return Pair("Android", LIBRARY_ANDROID_FRAGMENT)
        }
        if (line.startsWith("com.alibaba.sdk.android.push")) {
            return Pair("阿里巴巴", LIBRARY_ALIBABA_PUSH)
        }
        if (line.startsWith("pub.devrel.easypermissions")) {
            return Pair("三方", LIBRARY_EASY_PERMISSION)
        }
        if (line.startsWith("com.sensorsdata.analytics.android.sdk")) {
            return Pair("神策", LIBRARY_SENSORS)
        }
        if (line.startsWith("com.sina")
            || line.startsWith("com.weibo.ssosdk")
        ) {
            return Pair("新浪", LIBRARY_SINA)
        }
        if (line.startsWith("androidx.core")) {
            return Pair("Android", LIBRARY_ANDROID_CORE)
        }
        if (line.startsWith("com.alipay")) {
            return Pair("阿里巴巴", LIBRARY_ALIBABA_PAY)
        }
        if (line.startsWith("com.tencent.bugly")) {
            return Pair("腾讯", LIBRARY_TENCENT_BUGLY)
        }
        if (line.startsWith("com.tencent.connect")
            || line.startsWith("com.tencent.open")
            || line.startsWith("com.tencent.tauth")
        ) {
            return Pair("腾讯", LIBRARY_TENCENT_QQ)
        }
        if (line.startsWith("com.xiaomi.push")) {
            return Pair("小米", LIBRARY_XIAOMI_PUSH)
        }
        if (line.startsWith("com.tencent.mm.opensdk")) {
            return Pair("微信", LIBRARY_WX)
        }
        if (line.startsWith("com.hundsun")) {
            return Pair("恒生", LIBRARY_HUNDSUN)
        }
        if (line.startsWith("anet.channel")
            || line.startsWith("anetwork.channel")) {
            return Pair("阿里巴巴", LIBRARY_TAOBAO_ANDROID_NETWORK)
        }
        if (line.startsWith("com.taobao.accs")) {
            return Pair("阿里巴巴", LIBRARY_TAOBAO_ANDROID_ACCS)
        }
        val index = line.indexOf(" ")
        var clsName = line
        if (index != -1) {
            clsName = line.substring(0, index)
        }
        return Pair("-", clsName)
    }

    companion object {
        const val LIBRARY_ANDROID_FRAGMENT = "androidx.fragment:fragment"
        const val LIBRARY_ANDROID_CORE = "androidx.core:core"
        const val LIBRARY_ALIBABA_PUSH = "com.aliyun.ams:alicloud-android-push"
        const val LIBRARY_EASY_PERMISSION = "pub.devrel:easypermissions"
        const val LIBRARY_SENSORS = "com.sensorsdata.analytics.android:SensorsAnalyticsSDK"
        const val LIBRARY_SINA = "com.sina:weibo-core"
        const val LIBRARY_ALIBABA_PAY = "com.alipay"
        const val LIBRARY_TENCENT_BUGLY = "com.tencent.bugly:crashreport"
        const val LIBRARY_XIAOMI_PUSH = "com.xiaomi:push"
        const val LIBRARY_TENCENT_QQ = "com.tencent:qq"
        const val LIBRARY_WX = "com.tencent.mm.opensdk:wechat-sdk-android"
        const val LIBRARY_HUNDSUN = "com.hundsun"
        const val LIBRARY_TAOBAO_ANDROID_NETWORK = "com.taobao.android:networksdk"
        const val LIBRARY_TAOBAO_ANDROID_ACCS = "com.taobao.android:accs_sdk_taobao"

    }
}