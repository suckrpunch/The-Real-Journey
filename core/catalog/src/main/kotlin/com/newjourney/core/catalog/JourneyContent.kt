package com.newjourney.core.catalog

import android.content.Context
import com.newjourney.core.model.JourneyStage
import com.newjourney.core.model.StageContent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the words of The Journey Within from assets/journey/journey.json. */
object JourneyContentParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Map<JourneyStage, StageContent> {
        val file = json.decodeFromString<JourneyFile>(text)
        return file.stages.mapNotNull { dto ->
            val stage = JourneyStage.entries.firstOrNull { it.name == dto.id } ?: return@mapNotNull null
            stage to StageContent(stage, dto.name, dto.tagline, dto.reflection, dto.opens, dto.color)
        }.toMap()
    }

    @Serializable
    private data class JourneyFile(val stages: List<StageDto> = emptyList())

    @Serializable
    private data class StageDto(
        val id: String,
        val name: String,
        val tagline: String = "",
        val color: String = "",
        val reflection: String = "",
        val opens: String = "",
    )
}

@Singleton
class JourneyContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var cached: Map<JourneyStage, StageContent>? = null

    suspend fun content(): Map<JourneyStage, StageContent> = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            val text = context.assets.open("journey/journey.json").bufferedReader().use { it.readText() }
            JourneyContentParser.parse(text)
        }.also { cached = it }
    }
}
