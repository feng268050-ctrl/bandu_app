package com.bandu.tiji.core.common.result

sealed class AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>()

    data class Failure(val error: AppError) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    companion object {
        fun <T> catching(block: () -> T): AppResult<T> = try {
            Success(block())
        } catch (error: Throwable) {
            Failure(AppError.Unexpected(error))
        }
    }
}

sealed class AppError {
    data class Validation(val code: String) : AppError()

    data class NotFound(val resource: String) : AppError()

    data class PermissionDenied(val capability: String) : AppError()

    data class Network(val kind: NetworkKind) : AppError()

    data class Ai(val kind: AiKind) : AppError()

    data class Storage(val kind: StorageKind) : AppError()

    data class Transfer(val kind: TransferKind) : AppError()

    data class Unexpected(val cause: Throwable) : AppError()
}

enum class NetworkKind {
    TIMEOUT,
    UNREACHABLE,
    HTTP_ERROR,
}

enum class AiKind {
    NOT_CONFIGURED,
    AUTH,
    RATE_LIMITED,
    TIMEOUT,
    INVALID_RESPONSE,
}

enum class StorageKind {
    INSUFFICIENT_SPACE,
    IO_FAILURE,
}

enum class TransferKind {
    INTERRUPTED,
    CHECKSUM_FAILED,
    PROTOCOL_ERROR,
}
