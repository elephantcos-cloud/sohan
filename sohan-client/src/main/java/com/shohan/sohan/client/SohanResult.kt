package com.shohan.sohan.client

/**
 * Result wrapper returned by every [SohanClient] method.
 * No exceptions are thrown — all errors are wrapped here.
 */
sealed class SohanResult<out T> {

    /** The operation succeeded. [data] contains the return value. */
    data class Success<T>(val data: T) : SohanResult<T>()

    /** The operation failed. [message] is human-readable, [error] is the category. */
    data class Error<T>(
        val message: String,
        val error: SohanError = SohanError.UNKNOWN
    ) : SohanResult<T>()

    // ── Convenience helpers ───────────────────────────────────────────────────

    val isSuccess: Boolean get() = this is Success
    val isError:   Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrDefault(default: @UnsafeVariance T): T = (this as? Success)?.data ?: default

    fun errorMessage(): String? = (this as? Error)?.message

    /**
     * Transforms a [Success] value. Errors pass through unchanged.
     */
    fun <R> map(transform: (T) -> R): SohanResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error   -> Error(message, error)
    }

    /**
     * Runs [onSuccess] or [onError] depending on the result.
     */
    inline fun fold(
        onSuccess: (T) -> Unit,
        onError: (String, SohanError) -> Unit
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Error   -> onError(message, error)
        }
    }
}

/** Categories of errors that [SohanClient] can produce. */
enum class SohanError {
    /** Sohan app is not installed on this device. */
    NOT_INSTALLED,

    /** connect() was not called or binding failed. */
    NOT_BOUND,

    /** Connection timed out. */
    TIMEOUT,

    /** This app has not been authorized in Sohan's Service tab. */
    NOT_AUTHORIZED,

    /** Sohan's internal ADB session is disconnected. */
    ADB_DISCONNECTED,

    /** The privileged action itself failed (e.g. pm clear returned an error). */
    ACTION_FAILED,

    /** Unexpected error. Check [SohanResult.Error.message] for details. */
    UNKNOWN
}
