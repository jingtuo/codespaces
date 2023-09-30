package io.github.jingtuo.privacy.permission

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile

/**
 * 配置类
 */
abstract class PrivacyPermissionExtension {

    /**
     * cmdline-tools的目录
     */
    @InputDirectory
    abstract fun getCmdlineToolsDir(): DirectoryProperty

    /**
     * Apk文件, 注: 不是加固的apk
     */
    @InputFile
    abstract fun getApkFile(): RegularFileProperty

    /**
     * mapping文件
     */
    var mappingFilePath: String = ""

    /**
     * 权限描述文件, Json格式
     */
    @InputFile
    abstract fun getPermissionSpecsFile(): RegularFileProperty

    abstract fun getOutputFile(): RegularFileProperty

}