package io.github.jing.gitlab

import com.google.gson.annotations.SerializedName
import java.util.Date

data class MergeRequest(val id: Int, val iid: Int,  val projectId: Int) {
    var title: String = ""
    var description: String = ""
    var state: MrState = MrState.OPENED
    var mergedBy: User? = null
    var mergedAt: Date? = null
    var closedBy: User? = null
    var closedAt: Date? = null
    var createdAt: Date? = null
    var updatedAd: Date? = null
    var targetBranch: String = ""
    var sourceBranch: String = ""
    var author: User? = null
    var assignee: User? = null
    var assignees: Array<User>? = null
    var sourceProjectId: Int? = null
    var targetProjectId: Int? = null
    var labels: Array<String>? = null
    var mergeWhenPipelineSucceeds: Boolean = false
    var mergeStatus: String? = null
    var sha: String? = null
    var shouldRemoveSourceBranch: Boolean = false
    var forceRemoveSourceBranch: Boolean = false
    var webUrl: Boolean = false
    var squash: Boolean = false

    /**
     * 赞成数量
     */
    @SerializedName("upvotes")
    var upVotes: Int = 0

    /**
     * 反对数量
     */
    @SerializedName("downvotes")
    var downVotes: Int = 0

    var userNotesCount = 0


    var hasConflicts: Boolean = false

    var allowCollaboration: Boolean = false

    var allowMaintainerToPush: Boolean = false

    var changes: Array<MrChange>? = null
}
