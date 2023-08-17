package io.github.jing.tool

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * packageName: 应用ID
 * splashActivity: 应用启动的第一个页面
 * mainActivity: 应用的主页
 * 如果Splash页面的类包名包含应用ID, 需要使用相对路径
 */
class AppStartLogAnalysis(
    private val applicationId: String,
    private val splashActivity: String,
    private val mainActivity: String
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)

    /**
     * logFile: 日志文件
     * outputFile: 输出文件, *.csv
     */
    fun parse(logFile: File, outputFile: File) {
        if (!logFile.exists()) {
            return;
        }
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        val reader = BufferedReader(FileReader(logFile, StandardCharsets.UTF_8))
        var line: String?
        val writer = BufferedWriter(FileWriter(outputFile, StandardCharsets.UTF_8))
        val builder = StringBuilder()
        builder.append("应用启动时间, Splash显示时间, Splash显示耗时, Main启动事件, Main显示时间, Main显示耗时, 启动总时长")
            .append(LINE_SPLIT)
        writer.write(builder.toString())
        var appStart: Long? = null
        var splashDisplayed: Long? = null
        var mainStart: Long? = null
        var mainDisplayed: Long? = null
        var timeStr: String?
        var status: Int? = null
        while (true) {
            line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.contains(TAG_ACTIVITY_TASK_MANAGER, false)) {
                if (line.contains("$applicationId/$splashActivity", false)) {
                    //启动页
                    if (line.contains(MESSAGE_START)) {
                        //启动
                        builder.clear()
                        timeStr = getTimeStr(line)
                        appStart = getTime(timeStr)
                        builder.append("`").append(timeStr).append(CELL_SPLIT)
                        status = STATUS_APP_START
                    } else if (line.contains(MESSAGE_DISPLAYED)) {
                        //显示
                        if (STATUS_APP_START == status) {
                            //从应用启动到Slash显示
                            timeStr = getTimeStr(line)
                            splashDisplayed = getTime(timeStr)
                            builder.append("`").append(timeStr).append(CELL_SPLIT)
                                .append(splashDisplayed - appStart!!).append(CELL_SPLIT)
                            status = STATUS_SPLASH_DISPLAYED
                        }
                    }
                } else if (line.contains("$applicationId/$mainActivity", false)) {
                    //主页
                    if (line.contains(MESSAGE_START)) {
                        //启动
                        if (STATUS_APP_START == status) {
                            //未经过Splash显示
                            builder.append("-").append(CELL_SPLIT)
                                .append("-").append(CELL_SPLIT)
                            status = STATUS_SPLASH_DISPLAYED
                        }
                        if (STATUS_SPLASH_DISPLAYED == status) {
                            timeStr = getTimeStr(line)
                            mainStart = getTime(timeStr)
                            builder.append("`").append(timeStr).append(CELL_SPLIT)
                            status = STATUS_MAIN_START
                        }
                    } else if (line.contains(MESSAGE_DISPLAYED)) {
                        //显示
                        if (STATUS_MAIN_START == status) {
                            timeStr = getTimeStr(line)
                            mainDisplayed = getTime(timeStr)
                            builder.append("`").append(timeStr).append(CELL_SPLIT)
                                .append(mainDisplayed - mainStart!!).append(CELL_SPLIT)
                                .append(mainDisplayed - appStart!!).append(LINE_SPLIT)
                            writer.write(builder.toString())
                            status = STATUS_MAIN_DISPLAYED
                        }
                    }
                }
            }
        }
        writer.flush()
        writer.close()
        reader.close()
    }

    fun getTimeStr(line: String): String {
        var index = line.indexOf(" ");
        index = line.indexOf(" ", startIndex = index + 1);
        return line.substring(0, index)
    }

    fun getTime(dateStr: String): Long {
        return dateFormat.parse(dateStr).time
    }


    companion object {
        const val TAG_ACTIVITY_TASK_MANAGER = "ActivityTaskManager"
        const val TAG_WINDOW_MANAGER = "WindowManager"
        const val MESSAGE_START = " START "
        const val MESSAGE_DISPLAYED = " Displayed "
        const val MESSAGE_CHANGE_FOCUS_NULL_TO_WINDOW = "Changing focus from null to Window"
        const val CELL_SPLIT = ","
        const val LINE_SPLIT = "\n"
        const val STATUS_APP_START = 1
        const val STATUS_SPLASH_DISPLAYED = 2
        const val STATUS_MAIN_START = 3
        const val STATUS_MAIN_DISPLAYED = 4
    }

}