package io.github.jing.tool

import com.tinify.Source
import com.tinify.Tinify

class CompressImage {

    /**
     * 图片压缩, item是原始图片路径
     */
    fun compressByTinify(data: List<String>) {
        //设置KEY
        Tinify.setKey("6GtmT1klGtn210ZsSRlb7dZLkr7wYGql")
        val count = Tinify.compressionCount()
        val remainderCount = 500 - count
        println("remainder compress count $remainderCount")
        data.filter {
            val index = it.lastIndexOf(".")
            val ext = if (index == -1) {
                ""
            } else {
                it.substring(index)
            }
            ".png" == ext || ".jpg" == ext || ".webp" == ext
        }.map {
            val fromPath = it
            val index = it.lastIndexOf(".")
            var toPath = if (index == -1) {
                fromPath + "_compress"
            } else {
                it.substring(0, index) + "_compress" + it.substring(index)
            }
            Source.fromFile(fromPath).toFile(toPath)
        }
    }

}