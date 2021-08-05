package org.hatsuyuki.portalx.common

import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CoroutineSocketServer(port: Int) {
    private val channel: AsynchronousServerSocketChannel
    private val log = LogManager.getLogger()

    init {
        channel = AsynchronousServerSocketChannel.open()
        channel.bind(InetSocketAddress(port))
    }

    suspend fun accept() = suspendCoroutine<CoroutineSocket> { c ->
        channel.accept(Unit, object : CompletionHandler<AsynchronousSocketChannel, Unit> {
            override fun completed(result: AsynchronousSocketChannel, attachment: Unit) {
                c.resume(CoroutineSocket(result))
            }
            override fun failed(exc: Throwable, attachment: Unit) {
                c.resumeWithException(exc)
            }
        })
    }

    fun closeQuietly() {
        try {
            channel.close()
        } catch (e: Exception) {
            //ignored
        }
    }

}