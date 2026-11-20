package com.example.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    fun getFriendlyMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "لا يوجد اتصال بالإنترنت. يرجى التحقق من الشبكة."
            is SocketTimeoutException -> "انتهت مهلة الاتصال بالخادم. حاول مرة أخرى."
            is IOException -> "حدث خطأ في الاتصال بالشبكة. تأكد من اتصالك."
            else -> {
                val message = throwable.localizedMessage ?: "حدث خطأ غير معروف."
                when {
                    message.contains("401") -> "انتهت صلاحية الجلسة. يرجى تسجيل الدخول مجدداً."
                    message.contains("403") -> "ليس لديك صلاحية للقيام بهذا الإجراء."
                    message.contains("404") -> "البيانات المطلوبة غير موجودة."
                    message.contains("500") || message.contains("502") || message.contains("503") -> "خطأ في خادم النظام. يرجى المحاولة لاحقاً."
                    else -> message
                }
            }
        }
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
