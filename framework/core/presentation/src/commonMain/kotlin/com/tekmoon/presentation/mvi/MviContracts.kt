package com.tekmoon.presentation.mvi

/**
 * MVI marker interfaces for screen contracts.
 *
 * The framework base [com.tekmoon.presentation.viewmodel.CommonViewModel] is generic over
 * `<Action : Any, Event : Any, State : Any>`, so these markers are not strictly required by
 * the base — they exist to keep every feature's State/Action/Event consistent and greppable.
 */
interface UiState
interface UiAction
interface UiEvent

/** Marker for per-dialog state objects a ViewModel sends to the UI. */
interface UiDialogState
