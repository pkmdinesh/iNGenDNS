package com.ingendns.app.core

data class AppState(

    val running: Boolean = false,

    val currentDns: String = "",

    val connection: String = "NONE"

)