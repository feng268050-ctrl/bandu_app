package com.bandu.tiji.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val section = uiState.currentSection) {
        null -> ProfileOverview(onAction = onAction, modifier = modifier)
        ProfileSection.STUDENT -> StudentProfileScreen(
            uiState = uiState,
            onAction = onAction,
            modifier = modifier,
        )
        else -> ProfileSectionPlaceholder(
            section = section,
            onBack = { onAction(ProfileAction.Back) },
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileOverview(
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier,
) {
    BanduPageScaffold(title = "我的", modifier = modifier) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            items(ProfileSection.entries, key = ProfileSection::name) { section ->
                BanduCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile-section-${section.name.lowercase()}")
                        .clickable(
                            role = Role.Button,
                            onClick = { onAction(ProfileAction.OpenSection(section)) },
                        ),
                ) {
                    Text(section.title)
                    Text(section.description)
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionPlaceholder(
    section: ProfileSection,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    BanduPageScaffold(
        title = section.title,
        modifier = modifier,
        navigation = {
            Text(
                text = "返回",
                modifier = Modifier
                    .testTag("profile-back")
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(BanduSpacing.Small),
            )
        },
    ) { contentPadding ->
        Text(
            text = section.description,
            modifier = Modifier.padding(contentPadding).padding(BanduSpacing.PageHorizontal),
        )
    }
}
