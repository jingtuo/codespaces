package io.github.jing.tool

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * 扫描图片文件
 */
class ScanImage {

    /**
     * 扫描图片文件
     */
    fun scanImageFile(rootProjectFile: File, outputFile: File) {
        //收集图片文件
        val imageFiles = HashMap<String, MutableList<File>>()
        collectImageFiles(rootProjectFile, imageFiles)
        //收集使用情况
        val names = imageFiles.keys.toSet()
        val referenceFiles = HashMap<String, MutableList<File>>()
        collectReferenceFiles(rootProjectFile, names, referenceFiles)
        //输出文档
        writeOutputFile(imageFiles, referenceFiles, outputFile)
    }

    /**
     * 收集图片文件
     */
    private fun collectImageFiles(
        folder: File,
        result: HashMap<String, MutableList<File>>
    ) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val fileName = file.name
            if (file.isFile) {
                //文件
                if (file.parentFile.name.startsWith("drawable") || file.parentFile.name.startsWith("mipmap")) {
                    //位于布局目录下
                    var index = fileName.lastIndexOf(".")
                    var name = fileName
                    if (index != -1) {
                        name = fileName.substring(0, index)
                    }
                    index = name.lastIndexOf("_dark")
                    if (index != -1) {
                        name = fileName.substring(0, index)
                    }
                    index = name.lastIndexOf("_light")
                    if (index != -1) {
                        name = fileName.substring(0, index)
                    }
                    index = name.lastIndexOf("_day")
                    if (index != -1) {
                        name = fileName.substring(0, index)
                    }
                    index = name.lastIndexOf("_night")
                    if (index != -1) {
                        name = fileName.substring(0, index)
                    }
                    //添加到集合中
                    var list = result.computeIfAbsent(name) { ArrayList() }
                    list.add(file)
                }
            } else {
                //目录
                if ("build" == fileName
                    || "hs_trade_option" == fileName
                    || "hs_trade_stockoption" == fileName
                    || "hs_xianjinbao" == fileName
                    || "hs_quote_middlestage" == fileName
                    || "hs_quote_shcloud" == fileName
                ) {
                    //构建目录跳过
                    continue
                }
                collectImageFiles(file, result)
            }
        }
    }


    /**
     * 收集图片文件
     */
    private fun collectReferenceFiles(
        folder: File,
        names: Set<String>,
        result: HashMap<String, MutableList<File>>
    ) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val fileName = file.name
            if (file.isFile) {
                //文件
                if (fileName.endsWith(".java")
                    || fileName.endsWith(".kt")
                ) {
                    //检测是否有R.drawable.xxx
                    //检测是否有getIdentifier, 如果有, 需要人工确认
                    val content = readFile(file)
                    if (content.contains("getIdentifier")) {
                        System.err.println("please check file: ${file.absoluteFile}, because it contains getIdentifier")
                    }
                    for (name in names) {
                        if (content.contains("R.drawable.$name")
                            || content.contains("R.mipmap.$name")
                            || content.contains("skin:$name")
                        ) {
                            val list = result.computeIfAbsent(name) { ArrayList() }
                            list.add(file)
                        }
                    }
                } else if (fileName.endsWith(".xml")) {
                    val content = readFile(file)
                    for (name in names) {
                        if (content.contains("@drawable/$name")
                            || content.contains("@mipmap/$name")
                            || content.contains("skin:$name")
                        ) {
                            val list = result.computeIfAbsent(name) { ArrayList() }
                            list.add(file)
                        }
                    }
                }
            } else {
                //目录
                if ("build" == fileName
                    || "hs_trade_option" == fileName
                    || "hs_trade_stockoption" == fileName
                    || "hs_xianjinbao" == fileName
                    || "hs_quote_middlestage" == fileName
                    || "hs_quote_shcloud" == fileName
                ) {
                    //构建目录跳过
                    continue
                }
                collectReferenceFiles(file, names, result)
            }
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

    private fun writeOutputFile(
        imageFiles: Map<String, MutableList<File>>, referenceFiles: Map<String, MutableList<File>>,
        outputFile: File
    ) {
        try {
            BufferedWriter(FileWriter(outputFile)).use { writer ->
                //写入标题
                writer.write("name, file-path, file-size(KB), reference-count\n")
                var count = 0
                var totalFileSize = 0L
                for (name in imageFiles.keys) {
                    val list: List<File> = imageFiles[name]?.toList() ?: emptyList()
                    for (file in list) {
                        //file.length是字节的数量
                        writer.write(
                            "$name, ${file.absoluteFile}, " +
                                    "${String.format("%.2f", file.length() / 1024.0F)}," +
                                    "${referenceFiles[name]?.size ?: 0}\n"
                        )
                        totalFileSize += file.length()
                        count++
                    }
                }
                System.out.println("file count: $count, file total size: ${String.format("%.2f", totalFileSize / 1024.0F)}KB")
                writer.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}