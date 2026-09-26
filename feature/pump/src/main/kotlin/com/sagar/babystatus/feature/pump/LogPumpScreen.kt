package com.sagar.babystatus.feature.pump

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.component.BabyTextField
import com.sagar.babystatus.core.designsystem.component.CircleIconButton
import com.sagar.babystatus.core.designsystem.component.DangerTextButton
import com.sagar.babystatus.core.designsystem.component.MomBadge
import com.sagar.babystatus.core.designsystem.component.OptionButton
import com.sagar.babystatus.core.designsystem.component.OptionGrid
import com.sagar.babystatus.core.designsystem.component.PrimaryButton
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.domain.PumpSessionError
import com.sagar.babystatus.core.model.BreastSide
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.ui.DateTimeField
import com.sagar.babystatus.core.ui.VolumeSliderCard
import com.sagar.babystatus.core.ui.breastSideLabel
import com.sagar.babystatus.core.ui.format.VolumeScale
import com.sagar.babystatus.core.ui.unitName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun LogPumpRoute(onDone: () -> Unit, viewModel: LogPumpViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) currentOnDone()
    }
    LogPumpScreen(
        uiState = uiState,
        onClose = onDone,
        onVolumeChanged = viewModel::onVolumeChanged,
        onSideToggled = viewModel::onSideToggled,
        onDurationChanged = viewModel::onDurationChanged,
        onOccurredAtChanged = viewModel::onOccurredAtChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@Composable
internal fun LogPumpScreen(
    uiState: LogPumpUiState,
    onClose: () -> Unit,
    onVolumeChanged: (Double) -> Unit,
    onSideToggled: (BreastSide) -> Unit,
    onDurationChanged: (String) -> Unit,
    onOccurredAtChanged: (Instant) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        // Like SheetHeader, with the MOM badge above the title: this is Mom's entry.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                MomBadge(stringResource(R.string.feature_pump_mom), large = true)
                Text(
                    stringResource(if (uiState.isEdit) R.string.feature_pump_title_edit else R.string.feature_pump_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
            }
            CircleIconButton(
                icon = BabyIcons.Close,
                contentDescription = stringResource(R.string.feature_pump_close),
                onClick = onClose,
                containerColor = colors.closeButton,
            )
        }

        VolumeSliderCard(
            label = stringResource(R.string.feature_pump_amount),
            sliderDescription = stringResource(R.string.feature_pump_amount_description, uiState.scale.unitName()),
            scale = uiState.scale,
            value = uiState.volumeValue,
            onValueChange = onVolumeChanged,
            accent = colors.mom.border,
            track = colors.mom.bg,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_pump_side), style = MaterialTheme.typography.labelMedium, color = colors.stone)
            OptionGrid {
                BreastSide.entries.forEach { side ->
                    OptionButton(
                        label = breastSideLabel(side),
                        accent = colors.mom,
                        selected = uiState.side == side,
                        onClick = { onSideToggled(side) },
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BabyTextField(
                value = uiState.durationText,
                onValueChange = onDurationChanged,
                label = stringResource(R.string.feature_pump_duration),
                placeholder = "15",
                suffix = stringResource(R.string.feature_pump_minutes),
                error = if (uiState.durationInvalid) stringResource(R.string.feature_pump_duration_invalid) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            // No lower bound: some mothers pump before the birth.
            DateTimeField(
                label = stringResource(R.string.feature_pump_when),
                value = uiState.occurredAt,
                onValueChange = onOccurredAtChanged,
                zone = uiState.zone,
                today = uiState.today,
                minDate = null,
                error = if (PumpSessionError.IN_FUTURE in uiState.errors) stringResource(R.string.feature_pump_in_future) else null,
                modifier = Modifier.weight(1f),
            )
        }

        BabyTextField(
            value = uiState.note,
            onValueChange = onNoteChanged,
            label = stringResource(R.string.feature_pump_note),
            placeholder = stringResource(R.string.feature_pump_note_placeholder),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        Spacer(Modifier.height(4.dp))
        PrimaryButton(text = stringResource(R.string.feature_pump_save), onClick = onSave, enabled = uiState.canSave)
        if (uiState.isEdit) {
            DangerTextButton(text = stringResource(R.string.feature_pump_delete), onClick = onDelete)
        }
    }
}

@Preview(widthDp = 390, heightDp = 720)
@Composable
private fun LogPumpScreenPreview() {
    val zone = ZoneId.of("America/New_York")
    BabyStatusTheme {
        LogPumpScreen(
            uiState = LogPumpUiState(
                isEdit = false,
                isLoading = false,
                scale = VolumeScale.forUnits(UnitSystem.IMPERIAL),
                volumeValue = 3.0,
                side = BreastSide.BOTH,
                durationText = "15",
                durationInvalid = false,
                occurredAt = LocalDate.of(2026, 9, 24).atTime(15, 5).atZone(zone).toInstant(),
                note = "",
                errors = emptySet(),
                today = LocalDate.of(2026, 9, 24),
                zone = zone,
                isDone = false,
            ),
            onClose = {}, onVolumeChanged = {}, onSideToggled = {}, onDurationChanged = {},
            onOccurredAtChanged = {}, onNoteChanged = {}, onSave = {}, onDelete = {},
        )
    }
}
