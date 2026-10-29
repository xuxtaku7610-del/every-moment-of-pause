package com.emop.pause.trip

import android.content.Context
import com.emop.pause.BuildConfig
import com.emop.pause.engine.content.ContentLibrary
import com.emop.pause.engine.content.Episode
import com.emop.pause.engine.content.Mission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱에 묶은 콘텐츠 (assets/content/). 검수본 `content/episodes.json`·`missions.json`은 모든 빌드,
 * 초안 `content/drafts/`는 디버그 빌드 assets에만 있다 (app/build.gradle.kts). 서버 콘텐츠 API(/content/episodes 등)는 J에서 연결.
 */
@Singleton
class ContentRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private var cached: ContentLibrary? = null

    /** 처음 쓸 때 assets에서 읽는다. */
    val library: ContentLibrary
        get() = cached ?: load().also { cached = it }

    private fun load(): ContentLibrary =
        ContentLibrary.of(
            episodes("content/episodes.json"),
            if (BuildConfig.DEBUG) episodes("content/drafts/episodes.json") else emptyList(),
            missions("content/missions.json"),
            if (BuildConfig.DEBUG) missions("content/drafts/missions.json") else emptyList(),
            allowDrafts = BuildConfig.DEBUG,
        )

    private fun read(path: String): String? = runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull()

    private fun episodes(path: String): List<Episode> = read(path)?.let { runCatching { ContentLibrary.parseEpisodes(it) }.getOrNull() }.orEmpty()

    private fun missions(path: String): List<Mission> = read(path)?.let { runCatching { ContentLibrary.parseMissions(it) }.getOrNull() }.orEmpty()
}
