package org.hatsuyuki.portalx.common.handshake

import java.lang.Exception

class InvalidProtocolException(override val message: String) : Exception(message)