package com.sagar.babystatus.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.component.AccentButton
import com.sagar.babystatus.core.designsystem.component.BabyTextField
import com.sagar.babystatus.core.designsystem.component.CapsLabel
import com.sagar.babystatus.core.designsystem.component.CircleIconButton
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.ui.BabyPhoto
import com.sagar.babystatus.core.ui.DateField
import java.time.LocalDate

@Composable
internal fun ProfileRoute(onDone: () -> Unit, onBack: () -> Unit, viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) currentOnDone()
    }
    // The system Photo Picker needs no storage permission.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoPicked(it.toString()) }
    }
    ProfileScreen(
        uiState = uiState,
        onBack = onBack,
        onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onNameChanged = viewModel::onNameChanged,
        onBirthDateChanged = viewModel::onBirthDateChanged,
        onSave = viewModel::save,
    )
}

@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onNameChanged: (String) -> Unit,
    onBirthDateChanged: (LocalDate) -> Unit,
    onSave: () -> Unit,
) {
    val colors = BabyStatusTheme.colors
    val isCreate = uiState.mode == ProfileMode.CREATE
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.sage)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            HeaderBand(isCreate = isCreate, onBack = onBack)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            ) {
                PhotoPicker(uiState, onPickPhoto, Modifier.offset(y = (-40).dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().offset(y = (-40).dp),
                ) {
                    BabyTextField(
                        value = uiState.name,
                        onValueChange = onNameChanged,
                        label = stringResource(R.string.feature_profile_name),
                        placeholder = stringResource(R.string.feature_profile_name_placeholder),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    DateField(
                        label = stringResource(R.string.feature_profile_birth_date),
                        value = uiState.birthDate,
                        onValueChange = onBirthDateChanged,
                        today = uiState.today,
                        minDate = null,
                        placeholder = stringResource(R.string.feature_profile_birth_date_placeholder),
                        showRelativeDay = false,
                    )
                }
            }
        }
        AccentButton(
            text = stringResource(if (isCreate) R.string.feature_profile_continue else R.string.feature_profile_save),
            onClick = onSave,
            enabled = uiState.canSave,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 28.dp),
        )
    }
}

@Composable
private fun HeaderBand(isCreate: Boolean, onBack: () -> Unit) {
    val colors = BabyStatusTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(colors.forest)
            .statusBarsPadding(),
    ) {
        if (!isCreate) {
            CircleIconButton(
                icon = BabyIcons.Back,
                contentDescription = stringResource(R.string.feature_profile_back),
                onClick = onBack,
                contentColor = colors.ivory,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 48.dp, bottom = 76.dp),
        ) {
            if (isCreate) CapsLabel(stringResource(R.string.feature_profile_welcome), color = colors.mist)
            Text(
                stringResource(if (isCreate) R.string.feature_profile_title_create else R.string.feature_profile_title_edit),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ivory,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
}

@Composable
private fun PhotoPicker(uiState: ProfileUiState, onPickPhoto: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BabyStatusTheme.colors
    val hasPhoto = uiState.photo != null
    val label = stringResource(if (hasPhoto) R.string.feature_profile_change_photo else R.string.feature_profile_add_photo)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(112.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(if (hasPhoto) colors.goldLight else colors.cream)
                .clickable(onClickLabel = label, role = Role.Button, onClick = onPickPhoto)
                .then(
                    if (hasPhoto) {
                        Modifier
                    } else {
                        Modifier.drawBehind {
                            drawCircle(
                                color = colors.gold,
                                radius = size.minDimension / 2 - 1.dp.toPx(),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                                ),
                            )
                        }
                    },
                ),
        ) {
            if (hasPhoto) {
                BabyPhoto(source = uiState.photo, contentDescription = label, size = 104.dp, iconSize = 40.dp)
            } else {
                Icon(BabyIcons.Camera, contentDescription = label, tint = colors.hintIcon, modifier = Modifier.size(34.dp))
            }
        }
        Text(
            when {
                uiState.photoFailed -> stringResource(R.string.feature_profile_photo_failed)
                hasPhoto -> stringResource(R.string.feature_profile_change_photo)
                else -> stringResource(R.string.feature_profile_add_photo_hint)
            },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (uiState.photoFailed) FontWeight.SemiBold else FontWeight.Normal),
            color = if (uiState.photoFailed) colors.danger else colors.stone,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ProfileScreenPreview() {
    BabyStatusTheme {
        ProfileScreen(
            uiState = ProfileUiState(mode = ProfileMode.CREATE, isLoading = false, today = LocalDate.of(2026, 9, 24)),
            onBack = {}, onPickPhoto = {}, onNameChanged = {}, onBirthDateChanged = {}, onSave = {},
        )
    }
}
