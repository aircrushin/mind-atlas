package com.aircrushin.mindatlas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircrushin.mindatlas.ai.AiConfig
import com.aircrushin.mindatlas.ai.AiSettings
import com.aircrushin.mindatlas.ai.Brain
import com.aircrushin.mindatlas.data.Workspace
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextField
import kotlinx.coroutines.launch

@Composable
fun SettingsWindow(modifier: Modifier) {
    val cfg by AiSettings.config.collectAsState()
    var base by remember(cfg) { mutableStateOf(cfg.baseUrl) }
    var key by remember(cfg) { mutableStateOf(cfg.apiKey) }
    var model by remember(cfg) { mutableStateOf(cfg.model) }
    var offline by remember(cfg) { mutableStateOf(cfg.offlineOnly) }
    var test by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val secondary = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = PicoTheme.colorScheme.labelPrimary)

    Column(modifier.fillMaxSize()) {
        WindowHeader("设置", "AI 服务：任意 OpenAI 兼容接口（OpenRouter / 自建代理 / 国内大模型）")
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Label("接口地址 Base URL")
            TextField(value = base, onValueChange = { base = it }, modifier = Modifier.fillMaxWidth())
            Label("API Key（使用代理时可留空；仅保存在本机）")
            TextField(value = key, onValueChange = { key = it }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            Label("模型")
            TextField(value = model, onValueChange = { model = it }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { offline = !offline }, colors = secondary) { Text(if (offline) "☑ 仅离线模式" else "☐ 仅离线模式") }
                Button(onClick = {
                    test = "测试中…"
                    scope.launch { test = Brain.ping(AiConfig(base, key, model, false)) }
                }, colors = secondary) { Text("测试连接") }
            }
            test?.let { Text(it, fontSize = 14.sp, color = PicoTheme.colorScheme.labelSecondary) }
            Text(
                "没有网络或未配置时，应用会自动使用本地整理（关键词提取 + 相似度关联），核心流程始终可用。",
                fontSize = 13.sp, color = PicoTheme.colorScheme.labelTertiary,
            )
        }
        Row(Modifier.padding(28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { AiSettings.save(AiConfig(base, key, model, offline)); test = "已保存" }) { Text("保存") }
            Button(onClick = { AiSettings.resetToDefaults(); test = "已恢复默认" }, colors = secondary) { Text("恢复默认") }
            Button(onClick = { Workspace.resetAll(); test = "示例数据已重置" }, colors = secondary) { Text("重置示例数据") }
        }
    }
}

@Composable
private fun Label(t: String) = Text(t, fontSize = 13.sp, color = PicoTheme.colorScheme.labelSecondary)
