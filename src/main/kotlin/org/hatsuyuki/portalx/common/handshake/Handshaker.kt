package org.hatsuyuki.portalx.common.handshake

import org.hatsuyuki.portalx.common.Address
import org.hatsuyuki.portalx.common.Config
import org.hatsuyuki.portalx.common.CoroutineSocket
import org.hatsuyuki.portalx.common.Environment
import org.hatsuyuki.portalx.common.handshake.Authorizer.Result

class Handshaker {

    companion object {
        private val encoder = SocketMessageEncoder()
    }

    suspend fun doHandshakeToClient(clientSocket: CoroutineSocket, authorizer: Authorizer): Config {
        // authorize && reply
        val protocolSignature = encoder.readProtocolSignature(clientSocket)
        if (!encoder.checkProtocolSignature(protocolSignature)) {
            throw InvalidProtocolException("Invalid protocol signature: $protocolSignature")
        }
        val config = encoder.read<Config>(clientSocket)
        val authorizationResult = authorizer.check(config)
        val response = when(authorizationResult) {
            Result.OK -> HandshakeResponse(HandshakeResponse.Status.OK, "")
            Result.INVALID_TOKEN -> HandshakeResponse(HandshakeResponse.Status.NotAuthenticated, "Your token is invalid. Try logout and re-login")
            Result.NOT_AUTHORIZED -> HandshakeResponse(HandshakeResponse.Status.NotAuthorized, "You don't have permission to access this resource")
            Result.RESOURCE_NOT_FOUND -> HandshakeResponse(HandshakeResponse.Status.NotAuthorized, "The specified resource doesn't exist or was deleted")
        }
        encoder.write(clientSocket, response)

        // abort handshake if fail
        if (authorizationResult != Result.OK) {
            throw HandshakeException(response, config)
        }

        return config
    }

    suspend fun doHandshakeToForwarder(serverSocket: CoroutineSocket, config: Config) {
        encoder.writeProtocolSignature(serverSocket)
        encoder.write(serverSocket, config)
        val response = encoder.read<HandshakeResponse>(serverSocket)
        if (response.status != HandshakeResponse.Status.OK) {
            throw HandshakeException(response)
        }
    }
}