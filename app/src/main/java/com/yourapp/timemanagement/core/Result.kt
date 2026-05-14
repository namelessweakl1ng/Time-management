package com.yourapp.timemanagement.core

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val throwable: Throwable, val message: String = throwable.message ?: "Something went wrong") : Result<Nothing>
    data object Loading : Result<Nothing>
}

inline fun <T> runAsResult(block: () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (throwable: Throwable) {
        Result.Error(throwable)
    }
