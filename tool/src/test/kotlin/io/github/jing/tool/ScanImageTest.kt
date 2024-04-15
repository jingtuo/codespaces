package io.github.jing.tool

import java.io.File
import kotlin.test.Test


class ScanImageTest {

    @Test
    fun testScanImage() {
        val scanImage = ScanImage()
        scanImage.scanImageFile(File("D:\\Projects\\develop-20240318"), File("D:/文档/数据/图片扫描_7.13.0_20240415.csv"))
    }
}