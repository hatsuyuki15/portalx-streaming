package org.hatsuyuki.portalx.common

// API Model
data class Config(
    val token: String,
    val host: String,
    val port: Int,
    val environmentId: Int
)