package io.github.jing.gitlab


sealed class Sort(val name: String) {

    object ASC: Sort("asc")
    object DESC: Sort("desc")

    override fun toString(): String {
        return name
    }
}