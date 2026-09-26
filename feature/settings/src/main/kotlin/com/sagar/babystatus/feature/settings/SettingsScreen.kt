package com.sagar.babystatus.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagar.babystatus.core.designsystem.component.BabyTextField
import com.sagar.babystatus.core.designsystem.component.CapsLabel
import com.sagar.babystatus.core.designsystem.component.ForestTopBar
import com.sagar.babystatus.core.designsystem.component.IconBadge
import com.sagar.babystatus.core.designsystem.component.SegmentStyle
import com.sagar.babystatus.core.designsystem.component.SegmentedToggle
import com.sagar.babystatus.core.designsystem.component.WhiteCard
import com.sagar.babystatus.core.designsystem.icon.BabyIcons
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme
import com.sagar.babystatus.core.model.AppInfo
import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.model.UnitSystem
import com.sagar.babystatus.core.ui.BabyPhoto
import com.sagar.babystatus.core.ui.PhotoSource
import com.sagar.babystatus.core.ui.format.TimeFormatter
import java.time.LocalDate

@Composable
internal fun SettingsRoute(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val subject = stringResource(R.string.feature_settings_feedback_subject, uiState.appInfo.versionName)
    val noEmailApp = stringResource(R.string.feature_settings_no_email_app)
    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onEditProfile = onEditProfile,
        onUnitSystemSelected = viewModel::setUnitSystem,
        onRenameMother = viewModel::renameMother,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onSendFeedback = { context.sendFeedbackEmail(uiState.appInfo.feedbackEmail, subject, noEmailApp) },
    )
}

private fun Context.sendFeedbackEmail(address: String, subject: String, noEmailApp: String) {
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
        if (address.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, noEmailApp, Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onUnitSystemSelected: (UnitSystem) -> Unit,
    onRenameMother: (String) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onSendFeedback: () -> Unit,
) {
    val colors = BabyStatusTheme.colors
    Column(Modifier.fillMaxSize().background(colors.sage)) {
        ForestTopBar(
            title = stringResource(R.string.feature_settings_title),
            backLabel = stringResource(R.string.feature_settings_back),
            onBack = onBack,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.baby?.let { ProfileCard(it, onEditProfile) }
                MotherCard(uiState.motherName, onRenameMother)
            }
            UnitsSection(uiState.unitSystem, onUnitSystemSelected)
            AboutSection(uiState.appInfo, onOpenPrivacyPolicy, onSendFeedback)
        }
    }
}

@Composable
private fun ProfileCard(baby: Baby, onEditProfile: () -> Unit) {
    val colors = BabyStatusTheme.colors
    val editProfile = stringResource(R.string.feature_settings_edit_profile)
    WhiteCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = editProfile, role = Role.Button, onClick = onEditProfile)
                .padding(16.dp),
        ) {
            BabyPhoto(
                source = baby.photoPath?.let(PhotoSource::Stored),
                contentDescription = null,
                size = 56.dp,
                iconSize = 24.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(baby.name, style = MaterialTheme.typography.titleSmall.copy(fontSize = 18.sp), color = colors.ink)
                Text(
                    stringResource(R.string.feature_settings_born, TimeFormatter.fullDate(baby.birthDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.stone,
                )
            }
            Icon(BabyIcons.Chevron, contentDescription = null, tint = colors.chevron, modifier = Modifier.size(18.dp))
        }
    }
}

/** Mom's name (no mock yet): a row under the baby's card that opens a rename dialog. */
@Composable
private fun MotherCard(name: String, onRename: (String) -> Unit) {
    val colors = BabyStatusTheme.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    val rename = stringResource(R.string.feature_settings_rename_mom)
    WhiteCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = rename, role = Role.Button) { editing = true }
                .padding(16.dp),
        ) {
            IconBadge(
                icon = BabyIcons.Pump,
                containerColor = colors.mom.bg,
                contentColor = colors.momIcon,
                size = 56.dp,
                iconSize = 24.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.feature_settings_mom), style = MaterialTheme.typography.bodySmall, color = colors.stone)
                Text(name, style = MaterialTheme.typography.titleSmall.copy(fontSize = 18.sp), color = colors.ink)
            }
            Icon(BabyIcons.Chevron, contentDescription = null, tint = colors.chevron, modifier = Modifier.size(18.dp))
        }
    }
    if (editing) {
        RenameMotherDialog(
            current = name,
            onDismiss = { editing = false },
            onConfirm = {
                editing = false
                onRename(it)
            },
        )
    }
}

@Composable
private fun RenameMotherDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_settings_rename_mom)) },
        text = {
            BabyTextField(
                value = text,
                onValueChange = { text = it.take(SettingsViewModel.MAX_NAME_LENGTH) },
                label = stringResource(R.string.feature_settings_mom_name),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.feature_settings_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun UnitsSection(unitSystem: UnitSystem, onUnitSystemSelected: (UnitSystem) -> Unit) {
    val colors = BabyStatusTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.feature_settings_units))
        WhiteCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.feature_settings_measurement_system),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                SegmentedToggle(
                    options = listOf(
                        stringResource(R.string.feature_settings_imperial),
                        stringResource(R.string.feature_settings_metric),
                    ),
                    selectedIndex = unitSystem.ordinal,
                    onSelect = { onUnitSystemSelected(UnitSystem.entries[it]) },
                    style = SegmentStyle.Gold,
                    segmentHeight = 36.dp,
                    fillWidth = false,
                )
            }
        }
        Text(
            stringResource(R.string.feature_settings_units_hint),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = colors.stone,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun AboutSection(appInfo: AppInfo, onOpenPrivacyPolicy: () -> Unit, onSendFeedback: () -> Unit) {
    val colors = BabyStatusTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.feature_settings_about))
        WhiteCard {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    stringResource(R.string.feature_settings_version),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                )
                Text(appInfo.versionName, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = colors.stone)
            }
            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
            LinkRow(stringResource(R.string.feature_settings_privacy), onOpenPrivacyPolicy)
            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 16.dp))
            LinkRow(stringResource(R.string.feature_settings_feedback), onSendFeedback)
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    val colors = BabyStatusTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        Icon(BabyIcons.Chevron, contentDescription = null, tint = colors.chevron, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    CapsLabel(
        text,
        modifier = Modifier.semantics { heading() },
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    BabyStatusTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                baby = Baby("1", "Ava", LocalDate.of(2026, 6, 12), null),
                unitSystem = UnitSystem.IMPERIAL,
                motherName = "Mom",
                appInfo = AppInfo(versionName = "1.0.0", feedbackEmail = ""),
            ),
            onBack = {}, onEditProfile = {}, onUnitSystemSelected = {}, onRenameMother = {}, onOpenPrivacyPolicy = {},
            onSendFeedback = {},
        )
    }
}
