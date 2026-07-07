package dev.koukeneko.essentialkeytools.core

/**
 * A handle to a scheduled action that has not yet run. Cancelling it prevents the action from
 * firing; cancelling after it has already fired is a harmless no-op.
 */
interface Cancellable {
    fun cancel()
}

/**
 * Abstraction over delayed execution. The classifier depends on this instead of real timers so
 * that unit tests can drive time deterministically with a fake implementation. Production code
 * backs it with a Handler/coroutine; the core class stays free of Android and wall-clock timing.
 */
interface GestureScheduler {
    fun schedule(delayMs: Long, action: () -> Unit): Cancellable
}
