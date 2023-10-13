package io.github.jing.tool

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * 扫描布局文件
 *
 * @author JingTuo
 */
class ScanLayout {
    fun scanDuplicateLayoutFile(rootProjectFile: File, outputFile: File) {
        val result: MutableMap<String, MutableList<File>> = HashMap()
        scanDuplicateLayoutFile(rootProjectFile, result)
        writeOutputFile(result, outputFile)
    }

    private fun scanDuplicateLayoutFile(
        folder: File,
        result: MutableMap<String, MutableList<File>>
    ) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val fileName = file.name
            if (file.isFile) {
                //文件
                if (file.parentFile.name == "layout") {
                    //位于布局目录下
                    val index = fileName.lastIndexOf(".")
                    var name = fileName
                    if (index != -1) {
                        name = fileName.substring(0, index)
                    }
                    val list = result.computeIfAbsent(name) { ArrayList() }
                    //比较文件的内容
                    addToList(file, list)
                }
            } else {
                //目录
                if (fileName == "build" || fileName == "hs_trade_stockoption") {
                    //构建目录跳过
                    continue
                }
                scanDuplicateLayoutFile(file, result)
            }
        }
    }

    private fun addToList(file: File, list: MutableList<File>) {
        for (item in list) {
            val flag = equalsFileContent(file, item)
            if (flag) {
                return
            }
        }
        list.add(file)
    }

    /**
     * 判断两个文件的内容是否相同
     * @param file1
     * @param file2
     * @return
     */
    private fun equalsFileContent(file1: File, file2: File): Boolean {
        return try {
            val file1Content = readFile(file1)
            val file2Content = readFile(file2)
            file1Content == file2Content
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Throws(Exception::class)
    private fun readFile(file: File): String {
        try {
            BufferedReader(FileReader(file)).use { reader ->
                val builder = StringBuilder()
                while (true) {
                    val line = reader.readLine() ?: break
                    builder.append(line).append("\n")
                }
                return builder.toString()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun writeOutputFile(result: Map<String, MutableList<File>>, outputFile: File) {
        try {
            BufferedWriter(FileWriter(outputFile)).use { writer ->
                //写入标题
                writer.write("name\n")
                for (name in result.keys) {
                    val list: List<File> = result[name]!!
                    if (list.size >= 2) {
                        writer.write(
                            """
    $name
    
    """.trimIndent()
                        )
                    }
                }
                writer.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}