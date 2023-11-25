package io.github.jing.gitlab

import java.io.Serializable

/**
 * 这里不使用android.os.Parcelable, 是考虑模型可以用于Android之外
 */
class Branch(val name: String): Serializable {
    private var merged: Boolean = false
    var protected: Boolean = false
    private var default: Boolean = false
    private var developersCanPush: Boolean = false
    private var developersCanMerge: Boolean = false
    private var canPush: Boolean = false
    var commit: Commit? = null
}