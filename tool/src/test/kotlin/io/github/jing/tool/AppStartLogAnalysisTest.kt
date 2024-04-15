package io.github.jing.tool

import java.io.File
import kotlin.test.Test

internal class AppStartLogAnalysisTest() {

    val analysis = AppStartLogAnalysis("com.eno.android",
        "com.hundsun.winner.splash.activity.SplashActivity",
        "com.hundsun.main.activity.HsMainActivity")

    @Test
    fun parse() {
        analysis.parse(File("D:/文档/数据/20231107-1.log"), File("D:/文档/数据/20231107-2.csv"))
    }

}