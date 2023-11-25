package io.github.jing.gitlab

sealed class MrState(val name: String) {
    object ALL: MrState("all")
    object OPENED: MrState("opened")
    object CLOSED: MrState("closed")
    object MERGED: MrState("merged")

    override fun toString(): String {
        return name
    }
}
