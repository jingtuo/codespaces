package io.github.jing.gitlab

import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * page: 页码, 默认值1
 * per_page: 每页的数据个数, 默认值20, 最大值100
 */
interface GitlabApi {

    /**
     * membership=true, 表示成员(members)中有自己的工程
     */
    @GET("projects?simple=true")
    fun simpleProjects(
        @Query("membership") membership: Boolean = true,
        @Query("search") search: String = "",
        @Query("order_by") orderBy: String = "created_at",
        @Query("sort") sort: Sort = Sort.DESC,
        @Query("page") page: Int = 1,
        @Query("per_page") sizeOfPage: Int = 20
    ): Single<List<Project>>


    /**
     * scope可取值: projects, issues, merge_requests, milestones, snippet_titles, snippet_blobs, users.
     * scope不同, 返回结果不同
     */
    @GET("search?scope=projects")
    fun searchProjects(
        @Query("search") search: String,
        @Query("page") page: Int = 1,
        @Query("per_page") sizeOfPage: Int = 20
    ): Single<List<Project>>

    /**
     * MR审核通过
     */
    @POST("projects/{pid}/merge_requests/{mrIid}/approve")
    fun approveMR(@Path("pid") pid: Int, @Path("mrIid") mrIid: Int): Single<Int>

    /**
     * 撤销MR审核通过
     */
    @POST("projects/{pid}/merge_requests/{mrIid}/unapprove")
    fun unApproveMR(@Path("pid") pid: Int, @Path("mrIid") mrIid: Int): Single<Int>

    /**
     * 仅请求分配给当前用户的MR
     * scope支持: all, created_by_me, assigned_to_me
     * @param orderBy 可取值: created_at,updated_at, 默认值created_at
     */
    @GET("projects/{pid}/merge_requests")
    fun projectMR(
        @Path("pid") pid: Int,
        @Query("search") search: String, //匹配标题和描述
        @Query("state") state: MrState = MrState.ALL,
        @Query("order_by") orderBy: String = "created_at",
        @Query("sort") sort: Sort = Sort.DESC,
        @Query("page") page: Int = 1,
        @Query("per_page") sizeOfPage: Int = 20
    ): Single<List<MergeRequest>>

    @GET("projects/{pid}/merge_requests/{mrIid}/changes")
    fun mrChanges(@Path("pid") pid: Int, @Path("mrIid") mrIid: Int): Single<MergeRequest>

    @PUT("projects/{pid}/merge_requests/{mrIid}/merge")
    fun mergeMR(@Path("pid") pid: Int, @Path("mrIid") mrIid: Int): Single<MergeRequest>

    @PUT("projects/{pid}/merge_requests/{mrIid}/rebase")
    fun rebaseMR(@Path("pid") pid: Int, @Path("mrIid") mrIid: Int): Single<MrRebase>

    @PUT("projects/{pid}/merge_requests/{mrIid}")
    fun updateMR(
        @Path("pid") pid: Int, @Path("mrIid") mrIid: Int,
        @Field("title") title: String? = null,
        @Field("description") description: String? = null,
        @Field("state_event") stateEvent: String = "close" //close、reopen
    ): Single<MergeRequest>

    /**
     * 创建MR
     */
    @POST("projects/{pid}/merge_requests")
    fun createMr(
        @Path("pid") pid: Int,
        @Field("source_branch") sourceBranch: String,
        @Field("target_branch") targetBranch: String,
        @Field("title") title: String,
        @Field("description") description: String? = null,
        @Field("assignee_id") assigneeId: Int? = null
    ): Single<MergeRequest>


    /**
     * https://docs.gitlab.com/ee/api/projects.html#list-user-projects
     *
     * 用户创建的工程
     *
     * orderBy可取值: id,name,path,created_at,updated_at,last_activity_at
     */
    @GET("users/{userId}/projects")
    fun userProjects(
        @Path("userId") userId: Int,
        @Query("simple") simple: Boolean = true,
        @Query("membership") membership: Boolean = true,
        @Query("search") search: String = "",
        @Query("order_by") orderBy: String = "last_activity_at",
        @Query("sort") sort: Sort = Sort.DESC,
        @Query("page") page: Int = 1,
        @Query("per_page") sizeOfPage: Int = 20
    ): Single<List<Project>>

    @GET("users/{userId}/projects")
    fun userStarredProjects(
        @Path("userId") userId: Int,
        @Query("search") search: String = "",
        @Query("order_by") orderBy: String = "last_activity_at",
        @Query("sort") sort: Sort = Sort.DESC
    ): Single<List<Project>>

    /**
     * 搜索单个工程的用户
     */
    @GET("projects/{pid}/users")
    fun searchProjectUsers(
        @Path("pid") pid: Int, @Query("search") search: String = "",
        @Query("page") page: Int = 1,
        @Query("per_page") sizeOfPage: Int = 20
    ): Single<List<User>>

    /**
     * 搜索单个工程的分支
     */
    @GET("projects/{pid}/repository/branches")
    fun projectBranches(
        @Path("pid") pid: Int,
        @Query("search") search: String = "",
        @Query("page") page: Int = 1,
        @Query("per_page") sizeOfPage: Int = 20
    ): Single<List<Branch>>

    companion object {
        //头信息: token
        const val PRIVATE_TOKEN = "Private-Token"
    }

    /**
     * https://docs.gitlab.com/ee/api/users.html#list-current-user
     */
    @GET("user")
    fun currentUser(): Single<User>
}