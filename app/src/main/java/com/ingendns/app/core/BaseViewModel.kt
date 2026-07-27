package com.ingendns.app.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

abstract class BaseViewModel : ViewModel() {

    protected val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )
}