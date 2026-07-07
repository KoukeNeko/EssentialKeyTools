package dev.koukeneko.essentialkeytools.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

/**
 * Combines the Scaffold's system-bar [insets] with a uniform [screenPadding], so a screen keeps its
 * own breathing room while its content still clears the status and navigation bars. The result is
 * used as `contentPadding` on a scrolling list (content scrolls under the transparent bars, the
 * last item stays above the nav bar) or as plain padding on a static screen.
 */
@Composable
fun screenContentPadding(insets: PaddingValues, screenPadding: Dp): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return remember(insets, screenPadding, layoutDirection) {
        PaddingValues(
            start = insets.calculateStartPadding(layoutDirection) + screenPadding,
            top = insets.calculateTopPadding() + screenPadding,
            end = insets.calculateEndPadding(layoutDirection) + screenPadding,
            bottom = insets.calculateBottomPadding() + screenPadding
        )
    }
}
