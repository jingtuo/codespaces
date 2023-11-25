package io.github.jing.gitlab

import java.io.Serializable
import java.util.Date

class Commit(val id: String): Serializable {
    private var shortId: String? = null
    private var authorName: String? = null
    private var authorEmail: String? = null
    private var committedDate: Date? = null
    private var committerName: String? = null
    private var committerEmail: String? = null
    private var title: String? = null
    private var message: String? = null
}