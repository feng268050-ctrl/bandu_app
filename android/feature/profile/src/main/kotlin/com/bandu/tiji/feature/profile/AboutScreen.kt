package com.bandu.tiji.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bandu.tiji.core.designsystem.component.BanduCard
import com.bandu.tiji.core.designsystem.component.BanduPageScaffold
import com.bandu.tiji.core.designsystem.theme.BanduSpacing

@Composable
internal fun AboutScreen(
    aboutInfo: AboutInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BanduPageScaffold(
        title = "关于",
        modifier = modifier,
        navigation = {
            TextButton(onClick = onBack) {
                Text("返回")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = BanduSpacing.PageHorizontal),
            verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
        ) {
            item { AboutCard("应用名称", aboutInfo.appName) }
            item { AboutCard("版本", aboutInfo.versionName) }
            item { AboutCard("隐私说明", aboutInfo.privacyStatement) }
            item { AboutCard("开源许可", aboutInfo.openSourceLicenses) }
            item { AboutCard("图标来源", aboutInfo.iconSource) }
        }
    }
}

@Composable
private fun AboutCard(title: String, content: String) {
    BanduCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(content)
    }
}
