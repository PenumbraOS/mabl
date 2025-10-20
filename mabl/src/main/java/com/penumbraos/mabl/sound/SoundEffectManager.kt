package com.penumbraos.mabl.sound

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.util.Log
import java.io.File

private const val TAG = "SoundEffectManager"

class SoundEffectManager() {
    private val tonePlayer = TonePlayer()

    private val listeningMediaPlayer = MediaPlayer()
    private var listeningMediaPlayerReady = false

    @SuppressLint("SdCardPath")
    private val listeningSoundEffectFile = File("/sdcard/penumbra/mabl/sounds/listening.mp3")

    init {
        try {
            if (listeningSoundEffectFile.exists()) {
                listeningMediaPlayer.setDataSource(listeningSoundEffectFile.absolutePath)
                listeningMediaPlayer.setOnPreparedListener {
                    Log.d(TAG, "Loaded listening sound effect")
                    listeningMediaPlayerReady = true
                }
                listeningMediaPlayer.setOnErrorListener { player, what, extra ->
                    Log.e(TAG, "Error loading listening sound effect: $what, $extra")
                    false
                }
                listeningMediaPlayer.prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load listening sound effect", e)
        }
    }

    fun playStartListeningEffect() {
        tonePlayer.stop()
        stopStartListeningEffect()

        if (listeningSoundEffectFile.exists() && listeningMediaPlayerReady) {
            listeningMediaPlayer.start()
        } else {
            val g4 = TonePlayer.SoundEvent(
                doubleArrayOf(391.995),
                200,
                attackDurationMs = 50,
                releaseDurationMs = 50
            )

            tonePlayer.playJingle(
                listOf(g4)
            )
        }
    }

    fun stopStartListeningEffect() {
        // TODO: This might cause clicking
        tonePlayer.stop()
        if (listeningMediaPlayer.isPlaying) {
            listeningMediaPlayer.pause()
            listeningMediaPlayer.seekTo(0)
        }
    }

    fun playWaitingEffect() {
        tonePlayer.stop()

        val g4 = TonePlayer.SoundEvent(
            doubleArrayOf(391.995),
            200,
            attackDurationMs = 50,
            releaseDurationMs = 50
        )

        val bFlat4 = TonePlayer.SoundEvent(
            doubleArrayOf(466.164),
            200,
            attackDurationMs = 50,
            releaseDurationMs = 50
        )

        val c5 = TonePlayer.SoundEvent(
            doubleArrayOf(523.251),
            500,
            attackDurationMs = 50,
            releaseDurationMs = 50
        )

        tonePlayer.playJingle(
            listOf(
                g4, g4, g4, g4, g4,

                TonePlayer.SoundEvent.rest(600),

                bFlat4, bFlat4, bFlat4, bFlat4, bFlat4,

                TonePlayer.SoundEvent.rest(600),

                c5,

                TonePlayer.SoundEvent.rest(800),
            ),
            loop = true
        )
    }

    fun stopWaitingEffect() {
        // TODO: This might cause clicking
        tonePlayer.stop()
    }
}