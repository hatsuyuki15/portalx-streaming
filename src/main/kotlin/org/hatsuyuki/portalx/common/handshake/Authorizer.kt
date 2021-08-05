package org.hatsuyuki.portalx.common.handshake

interface Authorizer {
    suspend fun check(token: String, host: String, port: Int, environmentId: Int): Result

    data class Response(
        val result: Result
    )

    enum class Result {
        OK, INVALID_TOKEN, RESOURCE_NOT_FOUND, NOT_AUTHORIZED
    }
}