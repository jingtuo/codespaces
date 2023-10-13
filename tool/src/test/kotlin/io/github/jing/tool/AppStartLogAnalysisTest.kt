package io.github.jing.tool

import java.io.File
import kotlin.test.Test

internal class AppStartLogAnalysisTest() {

    val analysis = AppStartLogAnalysis("com.eno.android",
        "com.hundsun.winner.splash.activity.SplashActivity",
        "com.hundsun.main.activity.HsMainActivity")

    @Test
    fun parse() {
        analysis.parse(File("D:/文档/日志/20230818-2.log"), File("D:/文档/日志/20230818-2.csv"))
    }

}