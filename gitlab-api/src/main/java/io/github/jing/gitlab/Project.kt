package io.github.jing.gitlab

import java.util.Date


/**
 * 使用Gson解析, 不允许父类声明相同的属性
 * Caused by: java.lang.IllegalArgumentException:
 * Class io.github.jing.gitlab.Project declares multiple JSON fields named 'id';
 * conflict is caused by fields io.github.jing.gitlab.Project#id
 * and io.github.jing.work.assistant.gitlab.data.BaseData#id
 *
 * 如果有一个属性: id, kotlin不允许定义函数: fun getId(): Int
 */
data class Project(val id: Int, val name: String) {
    //simple
    var description: String = ""
    var defaultBranch: String? = null
    var sshUrlToRepo = ""
    var httpUrlToRepo = ""
    var webUrl = ""
    var readmeUrl = ""
    var tagList: Array<String>? = null
    var path: String = ""
    var pathWithNamespace: String = ""

    /**
     * 日期格式: 2023-05-15T08:28:20.640Z
     */
    var createdAt: Date? = null
    var lastActivityAt: Date? = null
    var avatarUrl: String? = null
    var forksCount: Int = 0
    var starCount: Int = 0


    //其他
    private var visibility: String? = null

}
