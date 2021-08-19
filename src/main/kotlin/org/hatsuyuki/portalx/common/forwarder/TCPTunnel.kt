package org.hatsuyuki.portalx.common.forwarder

import jdk.net.ExtendedSocketOptions
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.hatsuyuki.portalx.common.CoroutineSocket
import java.io.IOException
import java.net.StandardSocketOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class TCPTunnel(
    private val localSocket: CoroutineSocket,
    private val remoteSocket: CoroutineSocket,
    private val tunnelStatusListener: TunnelStatusListener,
    private val bufferSize: Int = 8192 * 4
) {
    private val log = LogManager.getLogger()
    private val isBroken = AtomicBoolean(false)

    companion object {
        private val dispatcher = Executors.newFixedThreadPool(8) {
            Thread(it).apply {
                isDaemon = true
            }
        }.asCoroutineDispatcher()
    }

    suspend fun start() {
        localSocket.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
        localSocket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 60)

        remoteSocket.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
        remoteSocket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 60)
        try {
            // Start forwarding data between server and client
            val clientForward = TCPForwarder(this, localSocket, remoteSocket, bufferSize)
            val serverForward = TCPForwarder(this, remoteSocket, localSocket, bufferSize)
            CoroutineScope(dispatcher).launch {
                clientForward.start()
            }
            CoroutineScope(dispatcher).launch {
                serverForward.start()
            }
        } catch (ioe: IOException) {
            log.error("Failed to connect to remote host", ioe)
            connectionBroken()
        }
    }

    suspend fun connectionBroken() {
        if (isBroken.compareAndSet(false, true)) {
            try {
                remoteSocket.close()
            } catch (e: Exception) {
            }
            try {
                localSocket.close()
            } catch (e: Exception) {
            }
            tunnelStatusListener.onBroken(this)
        }
    }
}