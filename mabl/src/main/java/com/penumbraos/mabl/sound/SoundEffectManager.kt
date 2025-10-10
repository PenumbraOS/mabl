package com.penumbraos.mabl.sound

class SoundEffectManager() {
    private val tonePlayer = TonePlayer()

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