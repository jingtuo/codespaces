package io.github.jing.gitlab

data class ApiConfig(val domainName: String = "gitlab.com", val ipAddress: String?,
                     val useHttps: Boolean = true, val apiVersion: String = "v4",
                     val personalAccessToken: String, val logEnabled: Boolean = false)