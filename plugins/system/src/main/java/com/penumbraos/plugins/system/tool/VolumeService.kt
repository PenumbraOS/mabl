package com.penumbraos.plugins.system.tool

import android.media.AudioManager
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolParameter
import com.penumbraos.mabl.sdk.ToolService
import org.json.JSONObject

private const val GET_VOLUME = "get_volume"
private const val SET_VOLUME = "set_volume"
private const val MUTE_VOLUME = "mute_volume"
private const val UNMUTE_VOLUME = "unmute_volume"
private const val INCREASE_VOLUME = "increase_volume"
private const val DECREASE_VOLUME = "decrease_volume"

class VolumeService : ToolService("VolumeService") {

    override fun executeTool(call: ToolCall, params: JSONObject?, callback: IToolCallback) {
        when (call.name) {
            GET_VOLUME -> {
                val volume = getVolume()
                callback.onSuccess("Device volume is $volume%")
            }

            SET_VOLUME -> {
                try {
                    val volume = call.parameters.toInt()
                    setVolume(volume)
                    callback.onSuccess("Volume set to $volume")
                } catch (_: Exception) {
                    callback.onError("Invalid volume value")
                    return
                }
            }

            MUTE_VOLUME -> {
                setMute(true)
                callback.onSuccess("Volume muted")
            }

            UNMUTE_VOLUME -> {
                setMute(false)
                callback.onSuccess("Volume unmuted")
            }

            INCREASE_VOLUME -> {
                val currentVolume = getVolume()
                val newVolume = (currentVolume + 10).coerceAtMost(100)
                setVolume(newVolume)
                callback.onSuccess("Volume increased to $newVolume%")
            }

            DECREASE_VOLUME -> {
                val currentVolume = getVolume()
                val newVolume = (currentVolume - 10).coerceAtLeast(0)
                setVolume(newVolume)
                callback.onSuccess("Volume decreased to $newVolume%")
            }
        }
    }

    override fun getToolDefinitions(): Array<ToolDefinition> {
        return arrayOf(ToolDefinition().apply {
            name = GET_VOLUME
            description = "Get the current volume level"
            examples = arrayOf(
                "volume",
                "what's the volume",
                "current volume level"
            )
            parameters = emptyArray()
        }, ToolDefinition().apply {
            name = SET_VOLUME
            description = "Set the volume level"
            parameters = arrayOf(ToolParameter().apply {
                name = "volume"
                type = "number"
                description = "Volume level to set as a percentage, 0-100"
                required = true
                enumValues = emptyArray()
            })
            examples = emptyArray()
        }, ToolDefinition().apply {
            name = MUTE_VOLUME
            description = "Mute the volume"
            examples = arrayOf(
                "mute the volume",
                "mute the device",
                "silence audio",
                "turn sound off"
            )
            parameters = emptyArray()
        }, ToolDefinition().apply {
            name = UNMUTE_VOLUME
            description = "Unmute the volume"
            examples = arrayOf(
                "unmute the volume",
                "unmute the device",
                "turn sound on"
            )
            parameters = emptyArray()
        }, ToolDefinition().apply {
            name = INCREASE_VOLUME
            description = "Increase the volume by 10%"
            examples = arrayOf(
                "increase the volume",
                "turn up the volume",
                "turn it up"
            )
            parameters = emptyArray()
        }, ToolDefinition().apply {
            name = DECREASE_VOLUME
            description = "Decrease the volume by 10%"
            examples = arrayOf(
                "decrease the volume",
                "turn down the volume",
                "turn it down"
            )
            parameters = emptyArray()
        })
    }

    fun getVolume(): Int {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return volume * 100 / maxVolume
    }

    fun setVolume(volume: Int) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume * maxVolume / 100,
            0
        )
    }

    fun setMute(muted: Boolean) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (muted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0
        )
    }
}