package org.hatsuyuki.portalx.common.handshake

import org.hatsuyuki.portalx.common.Config

interface Authorizer {
    suspend fun check(config: Config): Result

    data class Response(
        val result: Result
    )

    enum class Result {
        OK, INVALID_TOKEN, RESOURCE_NOT_FOUND, NOT_AUTHORIZED
    }
}