package org.hatsuyuki.portalx.common.forwarder

interface TunnelStatusListener {
    fun onBroken(tunnel: TCPTunnel)
}