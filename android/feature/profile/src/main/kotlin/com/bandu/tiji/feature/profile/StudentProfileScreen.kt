package com.bandu.tiji.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
internal fun StudentProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "学生资料",
        modifier = modifier,
        navigation = { ProfileBackButton(onAction) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            OutlinedTextField(
                value = uiState.studentDraft.nickname,
                onValueChange = { onAction(ProfileAction.UpdateNickname(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("昵称") },
                singleLine = true,
            )
            Text("教育阶段")
            EducationStages.forEach { stage ->
                FilterChip(
                    selected = uiState.studentDraft.educationStage == stage,
                    onClick = { onAction(ProfileAction.UpdateEducationStage(stage)) },
                    label = { Text(stage) },
                )
            }
            OutlinedTextField(
                value = uiState.studentDraft.enrollmentYear,
                onValueChange = { onAction(ProfileAction.UpdateEnrollmentYear(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("入学年份") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.studentErrorMessage != null,
                supportingText = uiState.studentErrorMessage?.let { message ->
                    { Text(message) }
                },
            )
            uiState.studentStatusMessage?.let { message ->
                Text(message)
            }
            Button(
                onClick = { onAction(ProfileAction.SaveStudentProfile) },
                enabled = !uiState.isSavingStudent,
            ) {
                Text(if (uiState.isSavingStudent) "保存中" else "保存")
            }
        }
    }
}

@Composable
internal fun ProfileBackButton(onAction: (ProfileAction) -> Unit) {
    Text(
        text = "返回",
        modifier = Modifier
            .clickable(role = Role.Button) { onAction(ProfileAction.Back) }
            .padding(BanduSpacing.Small),
    )
}
