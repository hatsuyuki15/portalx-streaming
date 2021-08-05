package org.hatsuyuki.portalx.common

import java.net.URI

data class Address(
    val host: String,
    val port: Int
) {
    companion object {
        fun fromUrl(url: String): Address {
            val uri = URI.create(url)
            return Address(uri.host, uri.port)
        }
    }
}