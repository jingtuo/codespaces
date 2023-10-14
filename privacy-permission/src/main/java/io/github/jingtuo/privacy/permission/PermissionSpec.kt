package io.github.jingtuo.privacy.permission

import kotlinx.serialization.Serializable

/**
 * 权限信息
 * name: 权限名称
 * clsName: 类名
 * propertyName: 属性名或方法名
 */
@Serializable
class PermissionSpec {
    var name: String = ""
    var clsName: String = ""
    var isField: Boolean = false
    var fieldName: String = ""
    var fieldType: String = ""
    var methodName: String = ""
    var paramTypes: Array<String>? = null
    var methodReturnType: String? = null

    fun getReferencesTo(): String {
        if (isField) {
            return "$clsName $fieldType $fieldName"
        }
        var result = "$clsName ${methodReturnType?:"void"} $methodName("
        paramTypes?.let {
            for ((index, paramType) in it.withIndex()) {
                result += paramType
                if (index != it.size - 1) {
                    result += ","
                }
            }
        }
        result += ")"
        return result
    }
}