package com.emop.pause.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.emop.pause.ui.theme.LocalSpacing

/** 출발 준비 (AppMode.PREPARE). 정차 중이므로 전체 화면 사용. */
@Composable
fun PrepareScreen(onStart: () -> Unit) {
    ScreenFrame(tag = "출발 준비") {
        Text("오늘도 같은 멤버로 출발할까요?", style = MaterialTheme.typography.headlineLarge)
        Text("어젯밤 몇 시간 주무셨어요?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(LocalSpacing.current.lg))
        BigButton("출발", onStart)
    }
}

/** 주행 글랜스 (AppMode.DRIVING). 텍스트 2줄 · 버튼 1개 · 애니메이션 없음. */
@Composable
fun GlanceScreen() {
    ScreenFrame(tag = "주행 중") {
        Text("다음 휴식까지 42분", style = MaterialTheme.typography.displayLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("피로 낮음 · 경로 정상", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
    }
}

/** 멈춤 화면 (AppMode.PAUSE). 애니메이션은 이 화면에서만 허용 (M3에서 벽난로). */
@Composable
fun PauseScreen() {
    ScreenFrame(tag = "멈춤", background = Color(0xFF1B120A)) {
        Text("잠깐 쉬어 가요", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text("충전 완료까지 27분 · 파워냅 · 원정대 · 펫 산책", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ScreenFrame(tag: String, background: Color = MaterialTheme.colorScheme.background, content: @Composable () -> Unit) {
    val sp = LocalSpacing.current
    // Surface가 LocalContentColor(onBackground)를 정해 줘야 어두운 배경에서 글자가 보인다.
    Surface(color = background, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(sp.xl),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            Text(tag, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun BigButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = LocalSpacing.current.touchTarget)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
