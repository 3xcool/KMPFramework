@file:Suppress(
    "LongMethod",
    "CyclomaticComplexMethod",
    "MaxLineLength",
    "MaximumLineLength",
)
// DsBottomSheet's gesture-handling pipeline is intrinsically large: it
// composes drag state, animation state, and three sheet positions. The
// long `when` chains and the 140+ char lines belong to a single, readable
// decision tree — extracting helpers would fragment the gesture logic and
// hurt review-time understanding.

package com.tekmoon.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── State ───────────────────────────────────────────────────────────────────

enum class DsBottomSheetState { COLLAPSED, HALF_EXPANDED, EXPANDED }

/**
 * Retained state holder for [DsBottomSheet].
 *
 * Create via [rememberDsBottomSheetScope] so screen-height is resolved from
 * the current composition and the instance survives recomposition.
 */
data class DsBottomSheetScope(
    val initialSheetState: DsBottomSheetState = DsBottomSheetState.COLLAPSED,
    val initialMinSheetHeightDp: Dp = 0.dp,
    val initialMaxSheetHeightDp: Dp = Int.MAX_VALUE.dp,
    val screenHeightDp: Dp,
    val hasHalfExpandedState: Boolean = false,
    val enableDragging: Boolean = true,
) {
    private val _sheetState = MutableStateFlow(initialSheetState)
    val sheetState = _sheetState.asStateFlow()

    private val _minSheetHeight = MutableStateFlow(initialMinSheetHeightDp)
    val minSheetHeight = _minSheetHeight.asStateFlow()

    private val _maxSheetHeight = MutableStateFlow(initialMaxSheetHeightDp)
    val maxSheetHeight = _maxSheetHeight.asStateFlow()

    val adjustedMaxSheetHeightDp: Dp get() = minOf(screenHeightDp, _maxSheetHeight.value)

    internal fun getInitialSheetHeight(): Dp = when (initialSheetState) {
        DsBottomSheetState.COLLAPSED     -> _minSheetHeight.value
        DsBottomSheetState.HALF_EXPANDED -> adjustedMaxSheetHeightDp / 2
        DsBottomSheetState.EXPANDED      -> adjustedMaxSheetHeightDp
    }

    internal fun getSheetHeight(): Dp = when (_sheetState.value) {
        DsBottomSheetState.COLLAPSED     -> _minSheetHeight.value
        DsBottomSheetState.HALF_EXPANDED -> adjustedMaxSheetHeightDp / 2
        DsBottomSheetState.EXPANDED      -> adjustedMaxSheetHeightDp
    }

    fun changeSheetState(newValue: DsBottomSheetState) { _sheetState.update { newValue } }
    fun expand()      { changeSheetState(DsBottomSheetState.EXPANDED) }
    fun halfExpand()  { changeSheetState(DsBottomSheetState.HALF_EXPANDED) }
    fun collapse()    { changeSheetState(DsBottomSheetState.COLLAPSED) }

    fun changeMinSheetHeight(newValue: Dp)    { _minSheetHeight.update { newValue } }
    fun changeMaxSheetHeight(newValue: Dp)    { _maxSheetHeight.update { newValue } }
    fun changeMinSheetHeight(newValue: Float) { _minSheetHeight.update { screenHeightDp * newValue } }
    fun changeMaxSheetHeight(newValue: Float) { _maxSheetHeight.update { screenHeightDp * newValue } }
}

@Composable
fun rememberDsBottomSheetScope(
    initialSheetState: DsBottomSheetState = DsBottomSheetState.COLLAPSED,
    minSheetHeightRelative: Float = 0f,
    maxSheetHeightRelative: Float = 1f,
    minSheetHeightDp: Dp? = null,
    maxSheetHeightDp: Dp? = null,
    hasHalfExpandedState: Boolean = false,
    enableDragging: Boolean = true,
): DsBottomSheetScope {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }

    return remember {
        DsBottomSheetScope(
            initialSheetState = initialSheetState,
            initialMinSheetHeightDp = minSheetHeightDp ?: (screenHeight * minSheetHeightRelative),
            initialMaxSheetHeightDp = maxSheetHeightDp ?: (screenHeight * maxSheetHeightRelative),
            screenHeightDp = screenHeight,
            hasHalfExpandedState = hasHalfExpandedState,
            enableDragging = enableDragging,
        )
    }
}

// ─── Component ───────────────────────────────────────────────────────────────

/**
 * Custom bottom sheet with three states (collapsed / half-expanded / expanded),
 * smooth drag gesture handling, and optional scrim.
 *
 * Ported from `CustomBottomSheet` and adapted to use DsTheme tokens.
 *
 * @param sheetScope        State holder — create with [rememberDsBottomSheetScope].
 * @param sheetContent      Content rendered inside the sheet.
 * @param modifier          Applied to the sheet surface (not the outer container).
 * @param sheetColor        Sheet background. Defaults to [DsTheme.surfaceColors.modal].
 * @param shape             Sheet shape. Bottom corners are always squared off.
 * @param showDragIndicator Whether to render the pill handle at the top.
 * @param handleColor       Handle pill color.
 * @param useDeltaMovement  `true` = snap based on drag delta; `false` = snap based on final position.
 * @param deltaMovementPrecision Drag delta threshold (px) required to trigger a state change.
 * @param hasScrim          Show a semi-transparent overlay behind the sheet.
 * @param scrimColor        Scrim overlay color.
 * @param onScrimClick      Called when the scrim is tapped. Defaults to collapsing the sheet.
 * @param onCollapse        Return `false` to veto a collapse gesture.
 * @param onHalfExpand      Return `false` to veto a half-expand gesture.
 * @param onExpand          Return `false` to veto an expand gesture.
 */
@Composable
fun DsBottomSheet(
    sheetScope: DsBottomSheetScope,
    modifier: Modifier = Modifier,
    sheetColor: Color = DsTheme.surfaceColors.modal,
    shape: Shape = DsTheme.shapes.xl,
    showDragIndicator: Boolean = true,
    handleColor: Color = DsTheme.colors.contentMuted.copy(alpha = 0.4f),
    useDeltaMovement: Boolean = true,
    deltaMovementPrecision: Float = 100f,
    hasScrim: Boolean = false,
    scrimColor: Color = DsTheme.colors.bgDark.copy(alpha = 0.6f),
    onScrimClick: (() -> Unit)? = null,
    onCollapse: () -> Boolean = { true },
    onHalfExpand: () -> Boolean = { true },
    onExpand: () -> Boolean = { true },
    sheetContent: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Morph shape so bottom corners are always flat (sheet slides from screen bottom)
    val morphedShape = remember(shape) {
        if (shape is CornerBasedShape)
            shape.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
        else shape
    }

    val initialSheetOffsetHeight by remember {
        mutableStateOf(sheetScope.screenHeightDp - sheetScope.getInitialSheetHeight())
    }

    var isDragging by remember { mutableStateOf(false) }
    var showScrim by remember { mutableStateOf(hasScrim && sheetScope.initialSheetState != DsBottomSheetState.COLLAPSED) }

    var sheetOffset by remember { mutableFloatStateOf(initialSheetOffsetHeight.value) }
    var prevSheetOffset by remember { mutableFloatStateOf(initialSheetOffsetHeight.value) }
    val animatedOffset by animateFloatAsState(targetValue = sheetOffset, label = "ds-sheet-offset")

    val getExpandedValue    = { sheetScope.screenHeightDp.value - sheetScope.adjustedMaxSheetHeightDp.value }
    val getHalfExpandedValue = { sheetScope.screenHeightDp.value - sheetScope.adjustedMaxSheetHeightDp.value / 2 }
    val getCollapsedValue   = { sheetScope.screenHeightDp.value - sheetScope.minSheetHeight.value.value }

    val returnPosition = { sheetOffset = prevSheetOffset }

    val onCollapseSheet = {
        if (onCollapse()) { sheetScope.changeSheetState(DsBottomSheetState.COLLAPSED); prevSheetOffset = sheetOffset }
        else returnPosition()
    }
    val onHalfExpandSheet = {
        if (onHalfExpand()) { sheetScope.changeSheetState(DsBottomSheetState.HALF_EXPANDED); prevSheetOffset = sheetOffset }
        else returnPosition()
    }
    val onExpandSheet = {
        if (onExpand()) { sheetScope.changeSheetState(DsBottomSheetState.EXPANDED); prevSheetOffset = sheetOffset }
        else returnPosition()
    }

    val sheetState by sheetScope.sheetState.collectAsState()
    val minHeight  by sheetScope.minSheetHeight.collectAsState()
    val maxHeight  by sheetScope.maxSheetHeight.collectAsState()

    val sheetHeight by remember(sheetState, isDragging, minHeight, maxHeight) {
        mutableStateOf(if (isDragging) sheetScope.adjustedMaxSheetHeightDp else sheetScope.getSheetHeight())
    }

    val animatedHeight by animateDpAsState(
        targetValue = sheetHeight,
        animationSpec = keyframes {
            durationMillis = 300
            val max = sheetScope.initialMaxSheetHeightDp
            if (isDragging) {
                sheetHeight at 0 with FastOutSlowInEasing
                sheetHeight at durationMillis
            } else {
                max at 0 with LinearEasing
                max at 280 with LinearEasing
                sheetHeight at durationMillis
            }
        },
        label = "ds-sheet-height"
    )

    LaunchedEffect(sheetState, minHeight, maxHeight) {
        sheetOffset = when (sheetState) {
            DsBottomSheetState.COLLAPSED     -> { showScrim = false;  getCollapsedValue() }
            DsBottomSheetState.HALF_EXPANDED -> { showScrim = true;   getHalfExpandedValue() }
            DsBottomSheetState.EXPANDED      -> { showScrim = true;   getExpandedValue() }
        }
        prevSheetOffset = sheetOffset
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Scrim
        if (hasScrim && showScrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onScrimClick?.invoke() ?: onCollapseSheet() }
            )
        }

        // Sheet surface
        Box(
            Modifier
                .heightIn(min = animatedHeight, max = animatedHeight)
                .graphicsLayer { translationY = with(density) { animatedOffset.dp.toPx() } }
                .fillMaxWidth()
                .background(sheetColor, shape = morphedShape)
                .then(modifier)
                .pointerInput(sheetScope.enableDragging) {
                    if (sheetScope.enableDragging) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onVerticalDrag = { change, dragAmount ->
                                val newPos = (sheetOffset + dragAmount / 2).coerceIn(
                                    0f,
                                    sheetScope.initialMaxSheetHeightDp.value
                                )
                                val aboveMin = newPos > sheetScope.screenHeightDp.value - sheetScope.maxSheetHeight.value.value
                                val belowMax = newPos < sheetScope.screenHeightDp.value - sheetScope.minSheetHeight.value.value
                                if (aboveMin && belowMax) {
                                    change.consume()
                                    sheetOffset = newPos
                                }
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    isDragging = false
                                    val delta = sheetOffset - prevSheetOffset
                                    if (useDeltaMovement) {
                                        when {
                                            delta > deltaMovementPrecision -> {
                                                when {
                                                    sheetScope.hasHalfExpandedState && prevSheetOffset == getExpandedValue() -> onHalfExpandSheet()
                                                    prevSheetOffset == getCollapsedValue() -> returnPosition()
                                                    else -> onCollapseSheet()
                                                }
                                            }
                                            delta < -deltaMovementPrecision -> {
                                                when {
                                                    sheetScope.hasHalfExpandedState && prevSheetOffset == getCollapsedValue() -> onHalfExpandSheet()
                                                    prevSheetOffset == getExpandedValue() -> returnPosition()
                                                    else -> onExpandSheet()
                                                }
                                            }
                                            else -> returnPosition()
                                        }
                                    } else {
                                        when {
                                            sheetOffset < sheetScope.adjustedMaxSheetHeightDp.value / 4 -> onExpandSheet()
                                            sheetScope.hasHalfExpandedState && sheetOffset < sheetScope.adjustedMaxSheetHeightDp.value / 2 -> onHalfExpandSheet()
                                            else -> onCollapseSheet()
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            if (showDragIndicator) {
                DsSheetDragIndicator(
                    sheetColor = sheetColor,
                    shape = morphedShape,
                    handleColor = handleColor,
                    content = sheetContent
                )
            } else {
                sheetContent()
            }
        }
    }
}

// ─── Internal ────────────────────────────────────────────────────────────────

@Composable
private fun DsSheetDragIndicator(
    sheetColor: Color,
    shape: Shape,
    handleColor: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(sheetColor, shape = shape)
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(color = handleColor, shape = CircleShape)
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}
