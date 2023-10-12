package io.github.jingtuo.privacy.permission

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

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
    @Input
    abstract fun getApkFilePath(): Property<String>

    /**
     * mapping文件
     */
    @Input
    abstract fun getMappingFilePath(): Property<String>

    /**
     * 权限描述文件, Json格式
     */
    @InputFile
    abstract fun getPermissionSpecsFile(): RegularFileProperty

    @OutputDirectory
    abstract fun getOutputDir(): DirectoryProperty


}