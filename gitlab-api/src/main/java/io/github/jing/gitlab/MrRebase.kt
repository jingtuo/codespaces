package io.github.jing.gitlab

class MrRebase {

    var rebaseInProgress: Boolean = false
    var mergeError: String? = null

    fun isSuccess(): Boolean {
        return !rebaseInProgress && mergeError.isNullOrEmpty()
    }

}