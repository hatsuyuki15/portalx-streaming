package org.hatsuyuki.portalx.common.forwarder

abstract class HierarchyEventListener<T>(private val parent: HierarchyEventListener<T>?) {
    protected suspend fun notifyParent(event: T) {
        try {
            parent?.handleEvent(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected open suspend fun handleEvent(event: T) {
        return notifyParent(event)
    }
}