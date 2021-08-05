package org.hatsuyuki.portalx.common

import java.util.*

fun <T> synchronizedMutableList() = Collections.synchronizedList(
    mutableListOf<T>()
)