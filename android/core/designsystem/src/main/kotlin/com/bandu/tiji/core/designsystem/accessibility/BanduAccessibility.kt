package com.bandu.tiji.core.designsystem.accessibility

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bandu.tiji.core.designsystem.DesignSystemModule

val BanduMinimumTouchTarget = DesignSystemModule.MinimumTouchTargetDp.dp

fun Modifier.banduMinimumTouchTarget(): Modifier =
    sizeIn(
        minWidth = BanduMinimumTouchTarget,
        minHeight = BanduMinimumTouchTarget,
    )
