package org.hatsuyuki.portalx.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class CoroutineSocket(
    private val socket: AsynchronousSocketChannel
) {
    var isConnected: Boolean = false
        private set
    val isOpened: Boolean
        get() = socket.isOpen

    val localAddress: SocketAddress
        get() = socket.localAddress

    val remoteAddress: SocketAddress
        get() = socket.remoteAddress

    open suspend fun connect(isa: InetSocketAddress) {
        suspendCoroutine<Void> {
            socket.connect(isa, it, ContinuationHandler<Void>())
        }
        isConnected = true
    }

    suspend fun readPartially(buffer: ByteBuffer, timeout: Duration? = null): Int {
        return suspendCoroutine {
            if (timeout != null) {
                socket.read(buffer, timeout.seconds,
                    TimeUnit.SECONDS, it, ContinuationHandler<Int>())
            } else {
                socket.read(buffer, it, ContinuationHandler<Int>())
            }
        }
    }

    suspend fun readFully(buffer: ByteBuffer, timeout: Duration) {
        var bytesRead: Int
        do {
            bytesRead = readPartially(buffer, timeout)
        } while (bytesRead > 0)
        if (buffer.hasRemaining()) {
            throw EOFException()
        }
    }

    suspend fun writePartially(buffer: ByteBuffer, timeout: Duration? = null): Int {
        return suspendCoroutine {
            if (timeout != null) {
                socket.write(buffer, timeout.seconds, TimeUnit.SECONDS, it, ContinuationHandler<Int>())
            } else {
                socket.write(buffer, it, ContinuationHandler<Int>())
            }
        }
    }

    suspend fun writeFully(buffer: ByteBuffer, timeout: Duration) {
        while (buffer.hasRemaining()) {
            writePartially(buffer)
        }
    }

    suspend fun readInt(timeout: Duration): Int {
        val bytes = ByteArray(4)
        val buffer = ByteBuffer.wrap(bytes)
        readFully(buffer, timeout)
        val ch1 = bytes[0].toUByte().toInt()
        val ch2 = bytes[1].toUByte().toInt()
        val ch3 = bytes[2].toUByte().toInt()
        val ch4 = bytes[3].toUByte().toInt()
        return (ch1 shl 24) + (ch2 shl 16) + (ch3 shl 8) + (ch4 shl 0)
    }

    suspend fun writeInt(v: Int, timeout: Duration) {
        val buffer = ByteBuffer.allocate(4)
        buffer.put((v ushr 24 and 0xFF).toByte())
        buffer.put((v ushr 16 and 0xFF).toByte())
        buffer.put((v ushr 8 and 0xFF).toByte())
        buffer.put((v ushr 0 and 0xFF).toByte())
        buffer.flip()
        writeFully(buffer, timeout)
    }


    open suspend fun close() {
        isConnected = false
        socket.shutdownInput()
        socket.shutdownOutput()
        socket.close()
    }

    suspend fun <T> setOption(option: SocketOption<T>, value: T) {
        withContext(Dispatchers.IO) {
            socket.setOption(option, value)
        }
    }

    override fun toString(): String {
        return "$remoteAddress/$localAddress"
    }

    class ContinuationHandler<T> : CompletionHandler<T, Continuation<T>> {
        override fun completed(result: T, attachment: Continuation<T>) {
            attachment.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Continuation<T>) {
            attachment.resumeWithException(exc)
        }
    }
}

suspend fun CoroutineSocket(address: Address) =
    CoroutineSocket(address.host, address.port)

suspend fun CoroutineSocket(host: String, port: Int): CoroutineSocket {
    val channel = withContext(Dispatchers.IO) {
        AsynchronousSocketChannel.open()
    }
    val socket = CoroutineSocket(channel)
    socket.connect(InetSocketAddress(host, port))
    return socket
}


