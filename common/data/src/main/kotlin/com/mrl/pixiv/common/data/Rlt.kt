package com.mrl.pixiv.common.data

open class IError(
    open val exception: Throwable,
) {
    override fun toString(): String {
        return "${this.javaClass.simpleName}(exception=$exception)"
    }
}

class NetworkHttpResBodyNullError(override val exception: Throwable) : IError(exception)

class NetworkHttpParseError(override val exception: Throwable) : IError(exception)

sealed class Rlt<out R> {
    data class Success<out T>(val data: T) : Rlt<T>()

    data class Failed(val error: IError) : Rlt<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Failed -> "Failed[error=$error]"
        }
    }
}