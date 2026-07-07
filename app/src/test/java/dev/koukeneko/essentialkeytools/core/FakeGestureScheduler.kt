package dev.koukeneko.essentialkeytools.core

/**
 * Deterministic test double for [GestureScheduler]. It does not use real time: scheduled actions
 * are stored with an absolute fire time and only run when the test explicitly advances the clock
 * via [advanceBy]. This lets timing tests assert exact window/threshold boundaries.
 */
class FakeGestureScheduler : GestureScheduler {

    private data class ScheduledAction(
        val fireAtMs: Long,
        val action: () -> Unit,
        var cancelled: Boolean = false
    )

    private var currentTimeMs = 0L
    private val pending = mutableListOf<ScheduledAction>()

    override fun schedule(delayMs: Long, action: () -> Unit): Cancellable {
        val scheduled = ScheduledAction(fireAtMs = currentTimeMs + delayMs, action = action)
        pending.add(scheduled)
        return object : Cancellable {
            override fun cancel() {
                scheduled.cancelled = true
            }
        }
    }

    /**
     * Advances virtual time and fires every non-cancelled action whose fire time has arrived,
     * in fire-time order. Actions scheduled while advancing are honoured on later ticks.
     */
    fun advanceBy(deltaMs: Long) {
        currentTimeMs += deltaMs
        var due = nextDueAction()
        while (due != null) {
            pending.remove(due)
            if (!due.cancelled) {
                due.action()
            }
            due = nextDueAction()
        }
    }

    private fun nextDueAction(): ScheduledAction? =
        pending
            .filter { !it.cancelled && it.fireAtMs <= currentTimeMs }
            .minByOrNull { it.fireAtMs }
}
