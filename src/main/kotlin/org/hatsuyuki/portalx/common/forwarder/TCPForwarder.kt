package org.hatsuyuki.portalx.common.forwarder

import org.hatsuyuki.portalx.common.CoroutineSocket
import java.io.IOException
import java.nio.ByteBuffer

class TCPForwarder(
    private val parent: TCPTunnel,
    private val `is`: CoroutineSocket,
    private val os: CoroutineSocket,
    private val bufferSize: Int
) {
    suspend fun start() {
        val bytes = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(bytes)
        try {
            while(buffer.position() > 0 || `is`.readPartially(buffer) >= 0) {
                buffer.flip();
                os.writePartially(buffer);
                buffer.compact();
            }
        } catch (e: IOException) {
            // Read/write failed --> connection is broken
        }
        //Notify the parent tunnel that the connection is broken
        parent.connectionBroken()
    }
}