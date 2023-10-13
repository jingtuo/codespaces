package io.github.jing.tool

import java.io.File
import kotlin.test.Test


class ScanImageTest {

    @Test
    fun testScanImage() {
        val scanImage = ScanImage()
        scanImage.scanImageFile(File("D:/Projects/feature-hilt"), File("D:/文档/数据/test.csv"))
    }
}