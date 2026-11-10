package com.example.browserpicker.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/** Size the icon is rasterised at. Independent from the size it is displayed at. */
private const val ICON_RASTER_PX = 96

/**
 * Renders a launcher icon.
 *
 * The drawable is rasterised once per [drawable] and reused, which keeps the
 * list smooth while scrolling.
 */
@Composable
fun BrowserIcon(
    drawable: Drawable,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val bitmap = remember(drawable) {
        drawable.toBitmap(width = ICON_RASTER_PX, height = ICON_RASTER_PX)
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}