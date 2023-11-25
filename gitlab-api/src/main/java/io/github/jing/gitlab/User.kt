package io.github.jing.gitlab

import java.io.Serializable
import java.util.Date

/**
 * name: 填写Full name
 */
data class User(val id: Int, val name: String): Serializable {
    /**
     * 用于网址一级path, 如https://gitlab.com/username
     */
    var username: String = ""

    /**
     * 邮箱
     */
    val email: String = ""

    /**
     * 状态
     */
    var state: String = ""
    var avatarUrl: String = ""
    var webUrl: String = ""
    var createdAt: Date? = null
    var location: String = ""
    var publicEmail: String = ""
    var organization: String = ""
    var lastSignInAt: Date? = null
    var currentSignInAt: Date? = null
    var canCreateGroup: Boolean = false
    var canCreateProject: Boolean = false
    var commitEmail: String = ""
    var twoFactorEnabled: Boolean = false
}
