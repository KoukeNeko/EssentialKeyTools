package dev.koukeneko.essentialkeytools.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val BAR_MARGIN = 16.dp
private val BAR_PADDING = 4.dp
private val TAB_SIZE = 56.dp
private val TAB_GAP = 8.dp
private val ICON_SIZE = 24.dp
private val RIM_WIDTH = 1.dp

// A press released further than this above or below the bar is a cancel, as on any other button.
private val CANCEL_SLOP = 48.dp

private val BLUR_RADIUS = 20.dp

// How much of the blurred screen shows through the glass; the rest is the bar's own tint.
private const val GLASS_ALPHA = 0.72f
private const val GLASS_TINT_ALPHA = 0.10f
private const val INDICATOR_ALPHA = 0.9f
private const val UNSELECTED_ICON_ALPHA = 0.72f
private const val RIM_BRIGHT_ALPHA = 0.35f
private const val RIM_DIM_ALPHA = 0.08f
private const val LIFTED_INDICATOR_SCALE = 1.12f

// Material 3 Expressive motion tokens. Position changes are spatial and may overshoot a little, while
// color is an effect that settles at once, so the tab under the finger reads as picked before the
// disc arrives. Following a finger needs a stiffer spring than settling on a tab.
private val SettleSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 380f)
private val FollowSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 1400f)
private val LiftSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 800f)
private val TintSpring = spring<Color>(dampingRatio = 1f, stiffness = 3800f)

/** One destination of [FloatingNavBar]: a line icon, and the name that screen readers announce. */
data class NavBarItem(@param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int)

/**
 * The screen content captured as a layer, so [FloatingNavBar] can draw a blurred copy of whatever
 * lies behind it. Create one with [rememberNavBarBackdrop], mark the content with
 * [navBarBackdropSource], and hand the same instance to the bar.
 */
@Stable
class NavBarBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var sourceOrigin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberNavBarBackdrop(): NavBarBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { NavBarBackdrop(layer) }
}

/**
 * Records the content into [backdrop] while still drawing it normally. The [background] is recorded
 * with it so the blurred copy is opaque and hides the sharp content underneath the bar.
 */
fun Modifier.navBarBackdropSource(backdrop: NavBarBackdrop, background: Color): Modifier =
    this
        .onGloballyPositioned { backdrop.sourceOrigin = it.positionInWindow() }
        .drawWithContent {
            backdrop.layer.record {
                drawRect(background)
                this@drawWithContent.drawContent()
            }
            drawLayer(backdrop.layer)
        }

/**
 * A frosted-glass capsule that floats above the content at the bottom of the screen, one icon per
 * destination. A dark disc behind the selected icon slides to the tab that is picked. Tapping a tab
 * picks it; pressing anywhere on the bar and dragging carries the disc along with the finger, and
 * the tab under the finger when it lifts is picked. Letting go well above or below the bar cancels.
 * The bar is only as wide as its icons, takes its colors from the color scheme so the Nothing and
 * Material You themes both fit, and is inset to sit in `Scaffold(bottomBar = ...)`, which then
 * reports its height to the screens as `innerPadding` so their content scrolls clear of it.
 *
 * Touch is handled by the bar as a whole, so each tab only exposes tab semantics, including the click
 * action that screen readers use. The icons are stroke-only vectors, so tinting them with the scheme
 * color recolors them completely.
 */
@Composable
fun FloatingNavBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: NavBarBackdrop,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val glassTint = colors.onSurface.copy(alpha = GLASS_TINT_ALPHA)
        .compositeOver(colors.surfaceContainer)
        .copy(alpha = GLASS_ALPHA)
    val rim = Brush.linearGradient(
        listOf(
            colors.onSurface.copy(alpha = RIM_BRIGHT_ALPHA),
            colors.onSurface.copy(alpha = RIM_DIM_ALPHA)
        )
    )
    val density = LocalDensity.current
    val tabSizePx = with(density) { TAB_SIZE.toPx() }
    val tabGapPx = with(density) { TAB_GAP.toPx() }
    val barPaddingPx = with(density) { BAR_PADDING.toPx() }
    val cancelSlopPx = with(density) { CANCEL_SLOP.toPx() }
    val tabStepPx = tabSizePx + tabGapPx
    val lastIndex = items.lastIndex

    val scope = rememberCoroutineScope()
    // The disc's left edge, in pixels along the row of tabs.
    val indicator = remember { Animatable(selectedIndex * tabStepPx) }
    var pressing by remember { mutableStateOf(false) }
    var hoverIndex by remember { mutableIntStateOf(selectedIndex) }
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val lift by animateFloatAsState(
        targetValue = if (pressing) LIFTED_INDICATOR_SCALE else 1f,
        animationSpec = LiftSpring,
        label = "indicatorLift"
    )
    // The tab being pointed at while a finger is down, otherwise the picked one.
    val highlightedIndex = if (pressing) hoverIndex else selectedIndex

    LaunchedEffect(selectedIndex, tabStepPx) {
        if (!pressing) {
            indicator.animateTo(selectedIndex * tabStepPx, SettleSpring)
        }
    }

    fun indexAt(x: Float): Int =
        floor((x - barPaddingPx + tabGapPx / 2) / tabStepPx).toInt().coerceIn(0, lastIndex)

    fun followFinger(x: Float) {
        val discLeft = (x - barPaddingPx - tabSizePx / 2).coerceIn(0f, lastIndex * tabStepPx)
        scope.launch { indicator.animateTo(discLeft, FollowSpring) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
            .padding(bottom = BAR_MARGIN),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .frostedGlass(backdrop, glassTint)
                .border(RIM_WIDTH, rim, CircleShape)
                .pointerInput(lastIndex, tabStepPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        pressing = true
                        hoverIndex = indexAt(down.position.x)
                        followFinger(down.position.x)
                        var last = down.position
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                ?: break
                            last = change.position
                            if (!change.pressed) break
                            hoverIndex = indexAt(last.x)
                            followFinger(last.x)
                            change.consume()
                        }
                        pressing = false
                        val cancelled = last.y < -cancelSlopPx || last.y > size.height + cancelSlopPx
                        val target = if (cancelled) currentSelected else indexAt(last.x)
                        if (target != currentSelected) {
                            currentOnSelect(target)
                        }
                        scope.launch { indicator.animateTo(target * tabStepPx, SettleSpring) }
                    }
                }
                .padding(BAR_PADDING)
        ) {
            // Moved in the draw phase, so following a finger never triggers composition or layout.
            Box(
                modifier = Modifier
                    .size(TAB_SIZE)
                    .graphicsLayer {
                        translationX = indicator.value
                        scaleX = lift
                        scaleY = lift
                    }
                    .background(colors.surface.copy(alpha = INDICATOR_ALPHA), CircleShape)
            )
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(TAB_GAP)
            ) {
                items.forEachIndexed { index, item ->
                    NavBarTab(
                        item = item,
                        selected = index == selectedIndex,
                        highlighted = index == highlightedIndex,
                        onActivate = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarTab(
    item: NavBarItem,
    selected: Boolean,
    highlighted: Boolean,
    onActivate: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        targetValue = if (highlighted) colors.onSurface else colors.onSurface.copy(alpha = UNSELECTED_ICON_ALPHA),
        animationSpec = TintSpring,
        label = "tabTint"
    )
    Box(
        modifier = Modifier
            .size(TAB_SIZE)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                onClick {
                    onActivate()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = stringResource(item.labelRes),
            tint = tint,
            modifier = Modifier.size(ICON_SIZE)
        )
    }
}

/**
 * Paints a blurred copy of [backdrop] under the node, then [tint] over it. The copy is recorded a few
 * blur radii larger than the node and clipped by the caller, so the blur is true right up to the edge
 * instead of smearing the border pixels.
 */
@Composable
private fun Modifier.frostedGlass(backdrop: NavBarBackdrop, tint: Color): Modifier {
    val blurLayer = rememberGraphicsLayer()
    val radius = with(LocalDensity.current) { BLUR_RADIUS.toPx() }
    SideEffect { blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val margin = radius * 3
    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawWithContent {
            val inBackdrop = origin - backdrop.sourceOrigin
            blurLayer.record(
                IntSize(
                    (size.width + 2 * margin).roundToInt(),
                    (size.height + 2 * margin).roundToInt()
                )
            ) {
                translate(margin - inBackdrop.x, margin - inBackdrop.y) { drawLayer(backdrop.layer) }
            }
            translate(-margin, -margin) { drawLayer(blurLayer) }
            drawRect(tint)
            drawContent()
        }
}
