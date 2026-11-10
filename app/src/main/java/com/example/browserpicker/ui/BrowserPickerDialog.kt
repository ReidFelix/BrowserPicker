package com.example.browserpicker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.browserpicker.R
import com.example.browserpicker.core.UrlFormatter
import com.example.browserpicker.model.BrowserInfo
import com.example.browserpicker.model.BrowserListUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the "copied" confirmation stays visible. */
private const val COPIED_FEEDBACK_MS = 1800L

/** Duration of the fade in / fade out of the whole picker. */
private const val ENTER_ANIMATION_MS = 160
private const val EXIT_ANIMATION_MS = 140

/** Opacity of the scrim that is drawn inside the dialog window. */
private const val SCRIM_ALPHA = 0.5f

/**
 * The dialog shown after the user tapped a link.
 *
 * The fade is driven from Compose rather than from a window animation. That is
 * not a stylistic choice: Compose's `Dialog` never dismisses its window —
 * `DialogWrapper.cancel()` is an empty method and `onDismissRequest` is called
 * straight from the touch/back handlers — so the window only disappears when
 * the Activity is destroyed. A window exit animation would therefore never be
 * seen, and the card would simply blink out.
 *
 * @param url the link that is about to be opened; tapping it copies it.
 * @param onDismiss called *after* the fade out has finished.
 */
@Composable
fun BrowserPickerDialog(
    url: String,
    uiState: BrowserListUiState,
    onDismiss: () -> Unit,
    onBrowserSelected: (BrowserInfo) -> Unit
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }

    // Switch off the platform window animation. The scrim covers the whole
    // screen, so the platform's scale-in/out would visibly shrink it.
    val view = LocalView.current
    SideEffect {
        // The dialog window is reachable through the DialogWindowProvider, which
        // Compose puts on the DialogLayout that hosts this composition.
        var candidate: Any? = view
        while (candidate != null) {
            val window = (candidate as? DialogWindowProvider)?.window
            if (window != null) {
                window.setWindowAnimations(0)
                break
            }
            candidate = (candidate as? android.view.View)?.parent
        }
    }

    // Fade in.
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(ENTER_ANIMATION_MS, easing = FastOutSlowInEasing))
    }

    val requestDismiss = {
        if (!dismissing) {
            dismissing = true
            scope.launch {
                progress.animateTo(0f, tween(EXIT_ANIMATION_MS, easing = FastOutSlowInEasing))
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = { requestDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // The scrim must reach behind the status/navigation bars.
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
        ) {
            // Purely visual scrim. It is drawn inside the dialog window so that it
            // fades out together with the card.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            )

            // The card sits between two flexible spacers. Tapping the empty
            // space above/below the card dismisses the picker, while taps on the
            // card itself are left completely untouched.
            //
            // A clickable scrim underneath the card would *not* work: Compose
            // delivers a tap to every hit node, and a consumed event still
            // reaches the sibling scrim, so the card could not swallow it.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { requestDismiss() }
                        )
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .graphicsLayer {
                            val scale = 0.94f + 0.06f * progress.value
                            scaleX = scale
                            scaleY = scale
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.choose_browser),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CopyableUrlRow(url = url)

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        BrowserListContent(
                            uiState = uiState,
                            onBrowserSelected = onBrowserSelected
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { requestDismiss() }
                        )
                )
            }
        }
    }
}

/**
 * Shows the URL and copies it to the clipboard when tapped.
 */
@Composable
private fun CopyableUrlRow(url: String) {
    var copied by remember(url) { mutableStateOf(false) }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS)
            copied = false
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (copied) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = stringResource(R.string.copy),
                    role = Role.Button
                ) {
                    if (copyToClipboard(context, url)) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        copied = true
                    }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = UrlFormatter.display(url),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            if (copied) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.copied),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.copy),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Copies [url] to the system clipboard.
 *
 * @return `true` when the clipboard accepted the value.
 */
private fun copyToClipboard(context: Context, url: String): Boolean {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return false
    val label = context.getString(R.string.clip_label)
    return try {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, url))
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun BrowserListContent(
    uiState: BrowserListUiState,
    onBrowserSelected: (BrowserInfo) -> Unit
) {
    when {
        // A list that is already there stays visible while it is refreshed.
        uiState.browsers.isNotEmpty() -> LazyColumn(
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            items(uiState.browsers, key = { it.packageName }) { browser ->
                BrowserListItem(
                    browser = browser,
                    onClick = { onBrowserSelected(browser) }
                )
            }
        }

        uiState.isLoading -> LoadingPlaceholder()

        else -> NoBrowsersPlaceholder(candidateCount = uiState.candidateCount)
    }
}

@Composable
private fun BrowserListItem(
    browser: BrowserInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrowserIcon(drawable = browser.icon)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = browser.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NoBrowsersPlaceholder(candidateCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_browsers),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_browsers_message),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (candidateCount > 0) {
            // Diagnostic: tells "the platform only returned us" apart from
            // "the platform returned nothing at all".
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.no_browsers_debug, candidateCount),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}