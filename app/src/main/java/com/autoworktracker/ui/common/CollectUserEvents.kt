package com.autoworktracker.ui.common

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.util.AppText
import kotlinx.coroutines.flow.Flow

@Composable
fun CollectUserEvents(
    events: Flow<UserEvent>,
    snackbarHostState: SnackbarHostState,
    onUndo: (Shift) -> Unit = {},
    text: AppText = AppText.of(AppLanguage.RU),
) {
    val context = LocalContext.current
    LaunchedEffect(events, snackbarHostState) {
        events.collect { event ->
            when (event) {
                is UserEvent.Snackbar -> snackbarHostState.showSnackbar(event.text)
                is UserEvent.Share -> shareSafely(context, snackbarHostState, event, text)
                is UserEvent.UndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = text.recordDeleted(),
                        actionLabel = text.undo(),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) onUndo(event.shift)
                }
            }
        }
    }
}

private suspend fun shareSafely(
    context: Context,
    snackbarHostState: SnackbarHostState,
    event: UserEvent.Share,
    text: AppText,
) {
    try {
        ShareCenter.share(
            context = context,
            message = event.message,
            attachment = event.attachment,
            chooserTitle = text.shareChooser(),
            subject = text.clipboardTitle,
        )
    } catch (_: Exception) {
        snackbarHostState.showSnackbar(text.shareFailed())
    }
}
