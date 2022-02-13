package moe.sdl.tracks.config

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import moe.sdl.tracks.consts.TRACKS_CONFIG_FILE

internal val tracksPreference: TracksPreference =
    runBlocking { getOrCreatePreference(TracksPreference()) }.apply {
        addShutdownSaveHook()
    }

@Serializable
internal class TracksPreference(
    val enableColor: Boolean = true,
) : Preference() {
    @Transient
    override val file: File = File(TRACKS_CONFIG_FILE)
}
