package com.ingendns.app.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {

    val io: CoroutineDispatcher

    val default: CoroutineDispatcher

    val main: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {

    override val io = Dispatchers.IO

    override val default = Dispatchers.Default

    override val main = Dispatchers.Main
}