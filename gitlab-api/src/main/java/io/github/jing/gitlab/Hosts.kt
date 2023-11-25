package io.github.jing.gitlab

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

class Hosts(private val config: Map<String, String>) : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        val ip = config[hostname]
        if (!ip.isNullOrEmpty()) {
            val array = ip.split(".")
            val byteArray = ByteArray(array.size)
            for ((i, item) in array.withIndex()) {
                byteArray[i] = (Integer.parseInt(item) and 0xFF).toByte()
            }
            return listOf(InetAddress.getByAddress(byteArray))
        }
        try {
            return InetAddress.getAllByName(hostname).toList()
        } catch (e: NullPointerException) {
            throw UnknownHostException("Broken system behaviour for dns lookup of $hostname").apply {
                initCause(e)
            }
        }
    }
}