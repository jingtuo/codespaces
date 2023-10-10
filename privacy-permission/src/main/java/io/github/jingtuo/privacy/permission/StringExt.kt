package io.github.jingtuo.privacy.permission


/**
 * 以某个字符开头的数量
 */
fun String.countStartsWith(char: Char): Int {
    val array = this.toCharArray()
    var count = 0
    for (item in array) {
        if (item == char) {
            count++
        } else {
            break
        }
    }
    return count
}