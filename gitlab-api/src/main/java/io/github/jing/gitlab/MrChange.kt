package io.github.jing.gitlab

/**
 * Merge Request Change
 */
class MrChange {
    var oldPath: String? = null
    var newPath: String? = null
    var aMode: String? = null
    var bMode: String? = null
    var diff: String? = null
    var newFile: Boolean = false
    var renamedFile: Boolean = false
    var deletedFile: Boolean = false
}
