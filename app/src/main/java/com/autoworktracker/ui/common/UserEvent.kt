package com.autoworktracker.ui.common

import com.autoworktracker.domain.model.Shift

sealed interface UserEvent {
    data class Snackbar(val text: String) : UserEvent
    data class Share(val message: String, val attachment: String) : UserEvent
    data class UndoDelete(val shift: Shift) : UserEvent
}
