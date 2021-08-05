package org.hatsuyuki.portalx.common.handshake

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hatsuyuki.portalx.common.CoroutineSocket
import java.nio.ByteBuffer
import java.time.Duration

class SocketMessageEncoder {
    companion object {
        val PROTOCOL_SIGNATURE = byteArrayOf(0x5f, 0x2e, 0x5, 0x5) // random chosen
        val timeout = Duration.ofSeconds(60)!!
    }

    val maxDataSize = 4096 // avoid ddos when client sends arbitrary large dataSize
    val mapper = jacksonObjectMapper()

    suspend inline fun <reified T> read(socket: CoroutineSocket): T {
        return mapper.readValue(readToBytes(socket), T::class.java)
    }

    suspend fun readToBytes(socket: CoroutineSocket): ByteArray {
        val dataSize = socket.readInt(timeout)
        if (dataSize > maxDataSize || dataSize <= 0) {
            error("Invalid data size: $dataSize")
        }

        val data = ByteArray(dataSize)
        socket.readFully(ByteBuffer.wrap(data), timeout)
        return data
    }

    suspend fun readProtocolSignature(socket: CoroutineSocket): ByteArray {
        val protocolSignature = ByteArray(PROTOCOL_SIGNATURE.size)
        socket.readFully(ByteBuffer.wrap(protocolSignature), timeout)
        return protocolSignature
    }

    suspend inline fun <reified T> write(socket: CoroutineSocket, any: T) {
        val data = mapper.writeValueAsBytes(any)
        if (data.size > maxDataSize) {
            error("Cannot encode message larger than $maxDataSize: ${data.size}")
        }
        socket.writeInt(data.size, timeout)
        socket.writeFully(ByteBuffer.wrap(data), timeout)
    }

    suspend fun writeProtocolSignature(socket: CoroutineSocket) {
        socket.writeFully(ByteBuffer.wrap(PROTOCOL_SIGNATURE), timeout)
    }

    fun checkProtocolSignature(protocolSignature: ByteArray): Boolean {
        return protocolSignature.contentEquals(PROTOCOL_SIGNATURE)
    }
}