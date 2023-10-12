package io.github.jingtuo.privacy.permission

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 隐私权限插件
 */
class PrivacyPermissionPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin(PLUGIN_ANDROID_APPLICATION)) {
            throw RuntimeException("Please apply privacy permission plugin with plugin(com.android.application)")
        }
        //创建配置
        val extension = project.extensions.create("privacyPermission", PrivacyPermissionExtension::class.java)
        //创建隐私权限检测的任务
        project.tasks.register("checkPrivacyPermission", CheckPrivacyPermissionTask::class.java) {
            //设置分组
            it.group = "verification"
            it.getCmdlineToolsDir().set(extension.getCmdlineToolsDir())
            it.getApkFilePath().set(extension.getApkFilePath())
            it.getMappingFilePath().set(extension.getMappingFilePath())
            it.getPermissionSpecsFile().set(extension.getPermissionSpecsFile())
            it.getOutputDir().set(extension.getOutputDir())
        }
    }

    companion object {
        const val PLUGIN_ANDROID_APPLICATION = "com.android.application"
    }
}