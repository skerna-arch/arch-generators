package io.skerna.arch.eventbus

import io.vertx.core.eventbus.Message

interface EventBusExceptionHandler {
    /**
     * onError metodo invocado cunado una exception se produce al tratar de responder un mensaje al cliente
     * @param exception
     * @param message
     */
    fun onError(exception: Exception?, message: Message<*>?)
}
