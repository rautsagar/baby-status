package com.sagar.babystatus.feature.growth

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.component.BabyTextField
import com.sagar.babystatus.core.designsystem.component.DangerTextButton
import com.sagar.babystatus.core.designsystem.component.HintBox
import com.sagar.babystatus.core.designsystem.component.PrimaryButton
import com.sagar.babystatus.core.designsystem.component.SheetHeader
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.domain.LogEntryError
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.ui.DateField
import com.sagar.babystatus.core.ui.format.UnitFormatter
import java.time.LocalDate

@Composable
internal fun LogGrowthRoute(onDone: () -> Unit, viewModel: LogGrowthViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) currentOnDone()
    }
    LogGrowthScreen(
        uiState = uiState,
        onClose = onDone,
        onDateChanged = viewModel::onDateChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onLengthChanged = viewModel::onLengthChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@Composable
internal fun LogGrowthScreen(
    uiState: LogGrowthUiState,
    onClose: () -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onWeightChanged: (String) -> Unit,
    onLengthChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val invalid = stringResource(R.string.feature_growth_invalid)
    val numberStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        SheetHeader(
            title = stringResource(if (uiState.isEdit) R.string.feature_growth_title_edit else R.string.feature_growth_title),
            closeLabel = stringResource(R.string.feature_growth_close),
            onClose = onClose,
        )
        DateField(
            label = stringResource(R.string.feature_growth_date),
            value = uiState.date,
            onValueChange = onDateChanged,
            today = uiState.today,
            minDate = uiState.birthDate,
            error = if (LogEntryError.BEFORE_BIRTH in uiState.errors) stringResource(R.string.feature_growth_before_birth) else null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BabyTextField(
                value = uiState.weightText,
                onValueChange = onWeightChanged,
                label = stringResource(R.string.feature_growth_weight),
                placeholder = if (uiState.unitSystem == UnitSystem.IMPERIAL) "13.2" else "6.0",
                suffix = UnitFormatter.massUnit(uiState.unitSystem),
                error = if (uiState.weightInvalid) invalid else null,
                height = 56.dp,
                textStyle = numberStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            BabyTextField(
                value = uiState.lengthText,
                onValueChange = onLengthChanged,
                label = stringResource(R.string.feature_growth_length),
                placeholder = if (uiState.unitSystem == UnitSystem.IMPERIAL) "23.5" else "60.0",
                suffix = UnitFormatter.lengthUnit(uiState.unitSystem),
                error = if (uiState.lengthInvalid) invalid else null,
                height = 56.dp,
                textStyle = numberStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        HintBox(stringResource(R.string.feature_growth_hint))
        BabyTextField(
            value = uiState.note,
            onValueChange = onNoteChanged,
            label = stringResource(R.string.feature_growth_note),
            placeholder = stringResource(R.string.feature_growth_note_placeholder),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(4.dp))
        PrimaryButton(text = stringResource(R.string.feature_growth_save), onClick = onSave, enabled = uiState.canSave)
        if (uiState.isEdit) {
            DangerTextButton(text = stringResource(R.string.feature_growth_delete), onClick = onDelete)
        }
    }
}

@Preview(widthDp = 390, heightDp = 640)
@Composable
private fun LogGrowthScreenPreview() {
    BabyStatusTheme {
        LogGrowthScreen(
            uiState = LogGrowthUiState(
                isEdit = false,
                isLoading = false,
                date = LocalDate.of(2026, 9, 24),
                weightText = "13.2",
                lengthText = "",
                note = "",
                unitSystem = UnitSystem.IMPERIAL,
                weightInvalid = false,
                lengthInvalid = false,
                errors = emptySet(),
                today = LocalDate.of(2026, 9, 24),
                birthDate = LocalDate.of(2026, 6, 12),
                isDone = false,
            ),
            onClose = {}, onDateChanged = {}, onWeightChanged = {}, onLengthChanged = {}, onNoteChanged = {},
            onSave = {}, onDelete = {},
        )
    }
}
