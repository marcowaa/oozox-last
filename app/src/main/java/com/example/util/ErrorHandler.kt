package com.example.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ErrorHandler {

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    fun getFriendlyMessage(throwable: Throwable): String {
        return AppErrorMapper.fromThrowable(throwable)
    }

    fun handleError(throwable: Throwable, context: Context? = null) {
        val friendlyMessage = getFriendlyMessage(throwable)
        _errorEvents.tryEmit(friendlyMessage)
        context?.let {
            Toast.makeText(it, friendlyMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun handleMessage(message: String, context: Context? = null) {
        _errorEvents.tryEmit(message)
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_LONG).show()
        }
    }
}
