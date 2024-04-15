package io.github.jingtuo.privacy.permission


/**
 * 权限信息
 * name: 权限名称
 * clsName: 类名
 * propertyName: 属性名或方法名
 */
class PermissionSpec {
    var name: String = ""
    var clsName: String = ""
    var type: String = ""
    var fieldName: String = ""
    var fieldType: String = ""
    var methodName: String = ""
    var paramTypes: Array<String>? = null
    var methodReturnType: String? = null

    fun getReferencesTo(): String {
        if ("field".equals(type)) {
            //属性
            return "$clsName $fieldType $fieldName"
        }
        if ("method".equals(type)) {
            //方法
            var result = "$clsName ${methodReturnType ?: "void"} $methodName("
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
        var result = "$clsName void <init>("
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