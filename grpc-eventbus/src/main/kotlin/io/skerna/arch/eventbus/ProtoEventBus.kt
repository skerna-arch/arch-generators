package io.skerna.arch.eventbus

interface ProtoEventBus {
    /**
     * bindService, this method allow link handlers to proto name methods calls
     * this method by default initialize all Exception handler with default code -1
     * @see EventBusExceptionHandler args @EventBusExceptionHandler for attach global exception
     * handler.
     */
    fun autoBindService()

    /**
     * bindService, this method allow link handlers to proto name methods calls, also add global
     * exception handler
     * handler.
     * @param  eventBusExceptionHandler
     */
    fun bindService(eventBusExceptionHandler: EventBusExceptionHandler?)
}
