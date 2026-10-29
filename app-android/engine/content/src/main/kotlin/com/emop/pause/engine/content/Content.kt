package com.emop.pause.engine.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** database.md `content_episodes` / api_spec §2.5. [lines]는 TTS가 0.6초씩 쉬며 읽는 문장들. */
@Serializable
data class Quiz(val question: String, val choices: List<String>, val answerIndex: Int)

@Serializable
data class Episode(
    val id: String,
    val topic: String,
    val title: String,
    val durationSec: Int,
    val minAge: Int = 4,
    val maxAge: Int = 12,
    val reviewed: Boolean = false,
    /** 사실 확인이 필요한 문장이 있음 (검수 때 확인). */
    val check: Boolean = false,
    val lines: List<String> = emptyList(),
    val quiz: Quiz? = null,
) {
    fun fits(age: Int) = age in minAge..maxAge
}

/** database.md `missions` / api_spec §2.7. */
@Serializable
data class Mission(
    val id: String,
    /** rest_stop | charging */
    val context: String,
    val title: String,
    val instructions: String,
    val durationSec: Int,
    val minAge: Int = 4,
    val reviewed: Boolean = false,
)

/**
 * 재생할 수 있는 콘텐츠 모음.
 * 아이용 콘텐츠는 검수된 것(reviewed=true)만 재생한다 (CLAUDE.md 규칙 4). 디버그 빌드만 초안을 허용하고,
 * 그때 화면에 "DRAFT"를 표시한다 ([isDraft]) — COMMERCIAL_READINESS F P1, test_plan T5-8.
 */
class ContentLibrary(episodes: List<Episode>, missions: List<Mission>, val allowDrafts: Boolean) {
    val episodes: List<Episode> = episodes.filter { it.reviewed || allowDrafts }
    val missions: List<Mission> = missions.filter { it.reviewed || allowDrafts }

    fun isDraft(e: Episode) = !e.reviewed
    fun isDraft(m: Mission) = !m.reviewed

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        @Serializable private data class EpisodeDoc(val items: List<Episode>)
        @Serializable private data class MissionDoc(val items: List<Mission>)

        fun parseEpisodes(text: String): List<Episode> = json.decodeFromString(EpisodeDoc.serializer(), text).items
        fun parseMissions(text: String): List<Mission> = json.decodeFromString(MissionDoc.serializer(), text).items

        /**
         * 검수본(content/episodes.json 등)과 초안(content/drafts/)을 합친다. 같은 ID면 검수본이 이긴다.
         * 릴리스 빌드는 [allowDrafts]=false라 초안이 들어와도 걸러진다.
         */
        fun of(reviewedEpisodes: List<Episode>, draftEpisodes: List<Episode>, reviewedMissions: List<Mission>, draftMissions: List<Mission>, allowDrafts: Boolean) =
            ContentLibrary(
                (reviewedEpisodes + draftEpisodes).distinctBy { it.id },
                (reviewedMissions + draftMissions).distinctBy { it.id },
                allowDrafts,
            )
    }
}
