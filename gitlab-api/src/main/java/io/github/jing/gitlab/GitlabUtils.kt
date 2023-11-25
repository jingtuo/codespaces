package io.github.jing.gitlab

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date

/**
 * Gitlab工具
 */

fun create(config: ApiConfig): GitlabApi {
    var baseUrl = if (config.useHttps) {
        "https"
    } else {
        "http"
    }
    baseUrl += "://"
    val hosts = mutableMapOf<String, String>()
    baseUrl += config.domainName
    if (!config.ipAddress.isNullOrEmpty()) {
        hosts[config.domainName] = config.ipAddress
    }
    baseUrl += "/api/${config.apiVersion}/"
    val headers = Headers.Builder()
        .add(GitlabApi.PRIVATE_TOKEN, config.personalAccessToken)
        .build()
    val clientBuilder = OkHttpClient.Builder()
        .dns(Hosts(hosts))
        .addInterceptor(HeaderInterceptor(headers))
    if (config.logEnabled) {
        clientBuilder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
    }
    val client = clientBuilder.build()
    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Date::class.java, GitlabDateAdapter())
        .registerTypeAdapter(MrState::class.java, GitlabMrStateAdapter())
        .create()
    val retrofit = Retrofit.Builder()
        .addCallAdapterFactory(RxJava3CallAdapterFactory.createSynchronous())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client)
        .baseUrl(baseUrl)
        .build()
    return retrofit.create(GitlabApi::class.java)
}