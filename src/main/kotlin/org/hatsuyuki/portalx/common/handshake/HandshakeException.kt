package org.hatsuyuki.portalx.common.handshake

import org.hatsuyuki.portalx.common.Config

class HandshakeException(val response: HandshakeResponse, val config: Config? = null) : RuntimeException()