package com.shohan.sohan.adb
open class AdbException : Exception {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
    constructor() : super()
}
class AdbInvalidPairingCodeException : AdbException()
