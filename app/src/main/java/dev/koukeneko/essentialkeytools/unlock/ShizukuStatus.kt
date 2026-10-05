package dev.koukeneko.essentialkeytools.unlock

/** How far along the user is in letting this app act through Shizuku. */
enum class ShizukuStatus {
    /** Shizuku is not installed or its service is not started. */
    NOT_RUNNING,

    /** Shizuku is running and has not been asked for permission yet. */
    PERMISSION_REQUIRED,

    /** The user denied the request; it can only be changed from the Shizuku app. */
    PERMISSION_DENIED,

    /** Packages can be enabled and disabled. */
    READY
}
