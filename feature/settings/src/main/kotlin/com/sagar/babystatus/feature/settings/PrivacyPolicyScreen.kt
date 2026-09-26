package com.sagar.babystatus.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagar.babystatus.core.designsystem.component.ForestTopBar
import com.sagar.babystatus.core.designsystem.component.WhiteCard
import com.sagar.babystatus.core.designsystem.theme.BabyStatusTheme

/** The app is installed privately, so its policy lives in the app instead of on a website. */
@Composable
internal fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val colors = BabyStatusTheme.colors
    val body = stringResource(R.string.feature_privacy_body)
    Column(Modifier.fillMaxSize().background(colors.sage)) {
        ForestTopBar(
            title = stringResource(R.string.feature_privacy_title),
            backLabel = stringResource(R.string.feature_settings_back),
            onBack = onBack,
        )
        WhiteCard(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Text(
                AnnotatedString.fromHtml(body.replace("\n", "<br>")),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = colors.ink,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
