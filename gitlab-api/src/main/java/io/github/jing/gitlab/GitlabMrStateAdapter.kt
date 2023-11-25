package io.github.jing.gitlab

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class GitlabMrStateAdapter: JsonDeserializer<MrState> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?): MrState? {
        val str = json?.asString
        if (str == MrState.CLOSED.name) {
            return MrState.CLOSED
        }
        if (str == MrState.MERGED.name) {
            return MrState.MERGED
        }
        return MrState.OPENED
    }
}