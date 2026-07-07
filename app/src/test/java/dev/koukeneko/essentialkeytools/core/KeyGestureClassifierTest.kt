package dev.koukeneko.essentialkeytools.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeyGestureClassifierTest {

    private lateinit var scheduler: FakeGestureScheduler
    private val emitted = mutableListOf<KeyGesture>()

    // Local mirrors of the classifier's timing constants so tests read as boundary assertions
    // rather than magic numbers.
    private val longPressThresholdMs = KeyGestureClassifier.LONG_PRESS_THRESHOLD_MS
    private val multiTapWindowMs = KeyGestureClassifier.MULTI_TAP_WINDOW_MS

    @Before
    fun setUp() {
        scheduler = FakeGestureScheduler()
        emitted.clear()
    }

    private fun classifierWith(enabled: Set<KeyGesture>): KeyGestureClassifier =
        KeyGestureClassifier(
            enabledGestures = enabled,
            scheduler = scheduler,
            onGesture = { gesture -> emitted.add(gesture) }
        )

    private fun allGesturesClassifier(): KeyGestureClassifier =
        classifierWith(KeyGesture.entries.toSet())

    // A tap = key-down immediately followed by key-up, well under the long-press threshold.
    private fun tap(classifier: KeyGestureClassifier, atMs: Long) {
        classifier.onKeyDown(atMs)
        classifier.onKeyUp(atMs + 10)
    }

    @Test
    fun singleTap_emitsSinglePressAfterWindowExpires() {
        val classifier = allGesturesClassifier()

        tap(classifier, atMs = 0)
        assertTrue("Should wait the window before deciding single", emitted.isEmpty())

        scheduler.advanceBy(multiTapWindowMs)
        assertEquals(listOf(KeyGesture.SINGLE_PRESS), emitted)
    }

    @Test
    fun singleTap_emitsImmediatelyWhenMultiTapDisabled() {
        // Only SINGLE and LONG enabled: no reason to wait the multi-tap window.
        val classifier = classifierWith(setOf(KeyGesture.SINGLE_PRESS, KeyGesture.LONG_PRESS))

        tap(classifier, atMs = 0)

        assertEquals(listOf(KeyGesture.SINGLE_PRESS), emitted)
    }

    @Test
    fun doubleTap_emitsDoublePress() {
        val classifier = allGesturesClassifier()

        tap(classifier, atMs = 0)
        tap(classifier, atMs = 100)
        assertTrue(emitted.isEmpty())

        scheduler.advanceBy(multiTapWindowMs)
        assertEquals(listOf(KeyGesture.DOUBLE_PRESS), emitted)
    }

    @Test
    fun doubleTap_emitsImmediatelyWhenTripleDisabled() {
        // TRIPLE disabled: the second key-up can resolve DOUBLE without waiting the window.
        val classifier =
            classifierWith(setOf(KeyGesture.SINGLE_PRESS, KeyGesture.DOUBLE_PRESS))

        tap(classifier, atMs = 0)
        tap(classifier, atMs = 100)

        assertEquals(listOf(KeyGesture.DOUBLE_PRESS), emitted)
    }

    @Test
    fun tripleTap_emitsTriplePress() {
        val classifier = allGesturesClassifier()

        tap(classifier, atMs = 0)
        tap(classifier, atMs = 100)
        tap(classifier, atMs = 200)
        assertTrue(emitted.isEmpty())

        scheduler.advanceBy(multiTapWindowMs)
        assertEquals(listOf(KeyGesture.TRIPLE_PRESS), emitted)
    }

    @Test
    fun fourTaps_capAtTriplePress() {
        val classifier = allGesturesClassifier()

        tap(classifier, atMs = 0)
        tap(classifier, atMs = 80)
        tap(classifier, atMs = 160)
        tap(classifier, atMs = 240)

        scheduler.advanceBy(multiTapWindowMs)
        assertEquals(listOf(KeyGesture.TRIPLE_PRESS), emitted)
    }

    @Test
    fun heldPress_emitsLongPress() {
        val classifier = allGesturesClassifier()

        classifier.onKeyDown(0)
        assertTrue(emitted.isEmpty())

        scheduler.advanceBy(longPressThresholdMs)
        assertEquals(listOf(KeyGesture.LONG_PRESS), emitted)
    }

    @Test
    fun longPress_doesNotAlsoEmitTap() {
        val classifier = allGesturesClassifier()

        classifier.onKeyDown(0)
        scheduler.advanceBy(longPressThresholdMs)
        // The eventual key-up must not be counted as a tap.
        classifier.onKeyUp(longPressThresholdMs + 50)
        scheduler.advanceBy(multiTapWindowMs)

        assertEquals(listOf(KeyGesture.LONG_PRESS), emitted)
    }

    @Test
    fun tapsSeparatedByMoreThanWindow_areIndependentSingles() {
        val classifier = allGesturesClassifier()

        tap(classifier, atMs = 0)
        scheduler.advanceBy(multiTapWindowMs)
        assertEquals(listOf(KeyGesture.SINGLE_PRESS), emitted)

        tap(classifier, atMs = multiTapWindowMs + 100)
        scheduler.advanceBy(multiTapWindowMs)
        assertEquals(
            listOf(KeyGesture.SINGLE_PRESS, KeyGesture.SINGLE_PRESS),
            emitted
        )
    }

    @Test
    fun reset_cancelsPendingGestureAndClearsState() {
        val classifier = allGesturesClassifier()

        tap(classifier, atMs = 0)
        classifier.reset()

        // Nothing should fire from the cancelled window.
        scheduler.advanceBy(multiTapWindowMs)
        assertTrue("reset must cancel the pending multi-tap decision", emitted.isEmpty())

        // And the classifier is usable again afterwards.
        tap(classifier, atMs = 1000)
        scheduler.advanceBy(multiTapWindowMs)
        assertEquals(listOf(KeyGesture.SINGLE_PRESS), emitted)
    }

    @Test
    fun reset_cancelsPendingLongPress() {
        val classifier = allGesturesClassifier()

        classifier.onKeyDown(0)
        classifier.reset()

        scheduler.advanceBy(longPressThresholdMs)
        assertTrue(emitted.isEmpty())
    }
}
