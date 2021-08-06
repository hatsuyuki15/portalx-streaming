package org.hatsuyuki.portalx.common.forwarder

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
    private val tunnelStatusListener: TunnelStatusListener
) {
    private var active = false
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
        remoteSocket.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
        try {
            // Start forwarding data between server and client
            active = true
            val clientForward = TCPForwarder(this, localSocket, remoteSocket)
            val serverForward = TCPForwarder(this, remoteSocket, localSocket)
            CoroutineScope(dispatcher).launch {
                clientForward.start()
            }
            CoroutineScope(dispatcher).launch {
                serverForward.start()
            }
            log.debug("TCP Forwarding: ${localSocket.localAddress} -> ${remoteSocket.remoteAddress}")
        } catch (ioe: IOException) {
            log.error("Failed to connect to remote host")
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
            if (active) {
                log.debug("TCP Forwarding stopped.")
                active = false
            }
            tunnelStatusListener.onBroken(this)
        }
    }
}