package com.autoworktracker.ui.common

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.autoworktracker.domain.model.Shift
import kotlinx.coroutines.flow.Flow

@Composable
fun CollectUserEvents(
    events: Flow<UserEvent>,
    snackbarHostState: SnackbarHostState,
    onUndo: (Shift) -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(events, snackbarHostState) {
        events.collect { event ->
            when (event) {
                is UserEvent.Snackbar -> snackbarHostState.showSnackbar(event.text)
                is UserEvent.Share -> shareSafely(context, snackbarHostState, event)
                is UserEvent.UndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Запись удалена",
                        actionLabel = "Вернуть",
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
) {
    try {
        ShareCenter.share(context, event.message, event.attachment)
    } catch (_: Exception) {
        snackbarHostState.showSnackbar("Не удалось открыть меню «Поделиться»")
    }
}
