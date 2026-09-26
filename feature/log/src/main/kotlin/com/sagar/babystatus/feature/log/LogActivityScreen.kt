package com.sagar.babystatus.feature.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.component.BabyTextField
import com.sagar.babystatus.core.designsystem.component.DangerTextButton
import com.sagar.babystatus.core.designsystem.component.OptionButton
import com.sagar.babystatus.core.designsystem.component.OptionGrid
import com.sagar.babystatus.core.designsystem.component.PrimaryButton
import com.sagar.babystatus.core.designsystem.component.SegmentedToggle
import com.sagar.babystatus.core.designsystem.component.SelectableTile
import com.sagar.babystatus.core.designsystem.component.SheetHeader
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.domain.LogEntryError
import com.sagar.babystatus.core.model.ActivityType
import com.sagar.babystatus.core.model.DiaperKind
import com.sagar.babystatus.core.model.Reaction
import com.sagar.babystatus.core.model.SolidsAmount
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.ui.DateTimeField
import com.sagar.babystatus.core.ui.VolumeSliderCard
import com.sagar.babystatus.core.ui.unitName
import com.sagar.babystatus.core.ui.diaperKindLabel
import com.sagar.babystatus.core.ui.format.VolumeScale
import com.sagar.babystatus.core.ui.reactionLabel
import com.sagar.babystatus.core.ui.solidsAmountLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun LogActivityRoute(onDone: () -> Unit, viewModel: LogActivityViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) currentOnDone()
    }
    LogActivityScreen(
        uiState = uiState,
        onClose = onDone,
        onTypeSelected = viewModel::onTypeSelected,
        onFeedKindSelected = viewModel::onFeedKindSelected,
        onBottleChanged = viewModel::onBottleChanged,
        onFoodChanged = viewModel::onFoodChanged,
        onSolidsAmountSelected = viewModel::onSolidsAmountSelected,
        onReactionToggled = viewModel::onReactionToggled,
        onDiaperKindSelected = viewModel::onDiaperKindSelected,
        onOccurredAtChanged = viewModel::onOccurredAtChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@Composable
internal fun LogActivityScreen(
    uiState: LogActivityUiState,
    onClose: () -> Unit,
    onTypeSelected: (ActivityType) -> Unit,
    onFeedKindSelected: (FeedKind) -> Unit,
    onBottleChanged: (Double) -> Unit,
    onFoodChanged: (String) -> Unit,
    onSolidsAmountSelected: (SolidsAmount) -> Unit,
    onReactionToggled: (Reaction) -> Unit,
    onDiaperKindSelected: (DiaperKind) -> Unit,
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
        SheetHeader(
            title = stringResource(if (uiState.isEdit) R.string.feature_log_title_edit else R.string.feature_log_title),
            closeLabel = stringResource(R.string.feature_log_close),
            onClose = onClose,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
            SelectableTile(
                label = stringResource(R.string.feature_log_feed),
                icon = BabyIcons.Bottle,
                accent = colors.feed,
                selected = uiState.type == ActivityType.FEED,
                enabled = !uiState.isEdit,
                onClick = { onTypeSelected(ActivityType.FEED) },
                modifier = Modifier.weight(1f),
            )
            SelectableTile(
                label = stringResource(R.string.feature_log_diaper),
                icon = BabyIcons.Drop,
                accent = colors.diaper,
                selected = uiState.type == ActivityType.DIAPER,
                enabled = !uiState.isEdit,
                onClick = { onTypeSelected(ActivityType.DIAPER) },
                modifier = Modifier.weight(1f),
            )
        }

        when (uiState.type) {
            ActivityType.FEED -> FeedSection(
                uiState,
                onFeedKindSelected,
                onBottleChanged,
                onFoodChanged,
                onSolidsAmountSelected,
                onReactionToggled,
            )
            ActivityType.DIAPER -> OptionGrid {
                DiaperKind.entries.forEach { kind ->
                    OptionButton(
                        label = diaperKindLabel(kind),
                        accent = colors.diaper,
                        selected = uiState.diaperKind == kind,
                        onClick = { onDiaperKindSelected(kind) },
                        height = 56.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        DateTimeField(
            label = stringResource(R.string.feature_log_when),
            value = uiState.occurredAt,
            onValueChange = onOccurredAtChanged,
            zone = uiState.zone,
            today = uiState.today,
            minDate = uiState.birthDate,
            error = when {
                LogEntryError.IN_FUTURE in uiState.errors -> stringResource(R.string.feature_log_in_future)
                LogEntryError.BEFORE_BIRTH in uiState.errors -> stringResource(R.string.feature_log_before_birth)
                else -> null
            },
        )
        BabyTextField(
            value = uiState.note,
            onValueChange = onNoteChanged,
            label = stringResource(R.string.feature_log_note),
            placeholder = stringResource(R.string.feature_log_note_placeholder),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        Spacer(Modifier.height(4.dp))
        PrimaryButton(
            text = stringResource(
                if (uiState.type == ActivityType.FEED) R.string.feature_log_save_feed else R.string.feature_log_save_diaper,
            ),
            onClick = onSave,
            enabled = uiState.canSave,
        )
        if (uiState.isEdit) {
            DangerTextButton(text = stringResource(R.string.feature_log_delete), onClick = onDelete)
        }
    }
}

@Composable
private fun FeedSection(
    uiState: LogActivityUiState,
    onFeedKindSelected: (FeedKind) -> Unit,
    onBottleChanged: (Double) -> Unit,
    onFoodChanged: (String) -> Unit,
    onSolidsAmountSelected: (SolidsAmount) -> Unit,
    onReactionToggled: (Reaction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!uiState.isEdit) {
            SegmentedToggle(
                options = listOf(stringResource(R.string.feature_log_bottle), stringResource(R.string.feature_log_solids)),
                selectedIndex = uiState.feedKind.ordinal,
                onSelect = { onFeedKindSelected(FeedKind.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (uiState.feedKind) {
            FeedKind.BOTTLE -> VolumeSliderCard(
                label = stringResource(R.string.feature_log_amount),
                sliderDescription = stringResource(R.string.feature_log_amount_description, uiState.bottleScale.unitName()),
                scale = uiState.bottleScale,
                value = uiState.bottleValue,
                onValueChange = onBottleChanged,
                accent = BabyStatusTheme.colors.gold,
                track = BabyStatusTheme.colors.feed.bg,
            )
            FeedKind.SOLIDS -> SolidsFields(uiState, onFoodChanged, onSolidsAmountSelected, onReactionToggled)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SolidsFields(
    uiState: LogActivityUiState,
    onFoodChanged: (String) -> Unit,
    onSolidsAmountSelected: (SolidsAmount) -> Unit,
    onReactionToggled: (Reaction) -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BabyTextField(
                value = uiState.food,
                onValueChange = onFoodChanged,
                label = stringResource(R.string.feature_log_food),
                placeholder = stringResource(R.string.feature_log_food_placeholder),
                error = if (LogEntryError.MISSING_FOOD in uiState.errors) stringResource(R.string.feature_log_food_required) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            if (uiState.foodSuggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.foodSuggestions.forEach { food -> SuggestionChip(food) { onFoodChanged(food) } }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_log_amount_eaten), style = MaterialTheme.typography.labelMedium, color = colors.stone)
            OptionGrid {
                SolidsAmount.entries.forEach { amount ->
                    OptionButton(
                        label = solidsAmountLabel(amount),
                        accent = colors.feed,
                        selected = uiState.solidsAmount == amount,
                        onClick = { onSolidsAmountSelected(amount) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_log_reaction), style = MaterialTheme.typography.labelMedium, color = colors.stone)
            OptionGrid {
                Reaction.entries.forEach { reaction ->
                    OptionButton(
                        label = reactionLabel(reaction),
                        accent = when (reaction) {
                            Reaction.LIKED -> colors.liked
                            Reaction.DISLIKED -> colors.disliked
                            Reaction.ALLERGIC -> colors.allergic
                        },
                        selected = uiState.reaction == reaction,
                        onClick = { onReactionToggled(reaction) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    val colors = BabyStatusTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Text(
        label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = colors.ink,
        modifier = Modifier
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Preview(widthDp = 390, heightDp = 780)
@Composable
private fun LogActivityScreenPreview() {
    val zone = ZoneId.of("America/New_York")
    BabyStatusTheme {
        LogActivityScreen(
            uiState = LogActivityUiState(
                isLoading = false,
                isEdit = false,
                type = ActivityType.FEED,
                feedKind = FeedKind.SOLIDS,
                bottleScale = VolumeScale.forUnits(UnitSystem.IMPERIAL),
                bottleValue = 3.0,
                food = "",
                foodSuggestions = LogActivityViewModel.STARTER_FOODS,
                solidsAmount = SolidsAmount.SOME,
                reaction = Reaction.LIKED,
                diaperKind = DiaperKind.PEE,
                occurredAt = LocalDate.of(2026, 9, 24).atTime(15, 5).atZone(zone).toInstant(),
                note = "",
                errors = emptySet(),
                today = LocalDate.of(2026, 9, 24),
                zone = zone,
                birthDate = LocalDate.of(2026, 6, 12),
                isDone = false,
            ),
            onClose = {}, onTypeSelected = {}, onFeedKindSelected = {}, onBottleChanged = {}, onFoodChanged = {},
            onSolidsAmountSelected = {}, onReactionToggled = {}, onDiaperKindSelected = {}, onOccurredAtChanged = {},
            onNoteChanged = {}, onSave = {}, onDelete = {},
        )
    }
}
