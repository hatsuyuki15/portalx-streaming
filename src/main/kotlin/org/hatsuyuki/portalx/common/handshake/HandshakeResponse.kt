package org.hatsuyuki.portalx.common.handshake

data class HandshakeResponse(
    val status: Status,
    val message: String
) {
    enum class Status {
        OK, NotAuthenticated, NotAuthorized
    }
}