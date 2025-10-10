package com.penumbraos.mabl.sound

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val SAMPLE_RATE = 44100

class TonePlayer {
    data class SoundEvent(
        /**
         * Empty array implies a rest. A single value implies a single note. Multiple values implies a chord
         */
        val frequenciesHz: DoubleArray,
        val durationMs: Int,
        val attackDurationMs: Int = 200,
        val releaseDurationMs: Int = 200,
        val waveform: Waveform = Waveform.SINE,
        val harmonics: List<Pair<Int, Double>> = emptyList(),
        val detuneHz: Double = 0.0,
        /**
         * Can be negative for overlap
         */
        val offsetMs: Int = 0
    ) {
        companion object {
            fun rest(durationMs: Int) =
                SoundEvent(doubleArrayOf(), durationMs, attackDurationMs = 0, releaseDurationMs = 0)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SoundEvent

            if (durationMs != other.durationMs) return false
            if (attackDurationMs != other.attackDurationMs) return false
            if (releaseDurationMs != other.releaseDurationMs) return false
            if (detuneHz != other.detuneHz) return false
            if (offsetMs != other.offsetMs) return false
            if (!frequenciesHz.contentEquals(other.frequenciesHz)) return false
            if (waveform != other.waveform) return false
            if (harmonics != other.harmonics) return false

            return true
        }

        override fun hashCode(): Int {
            var result = durationMs
            result = 31 * result + attackDurationMs
            result = 31 * result + releaseDurationMs
            result = 31 * result + detuneHz.hashCode()
            result = 31 * result + offsetMs
            result = 31 * result + frequenciesHz.contentHashCode()
            result = 31 * result + waveform.hashCode()
            result = 31 * result + harmonics.hashCode()
            return result
        }
    }

    enum class Waveform {
        SINE, SQUARE, TRIANGLE, SAWTOOTH;

        fun sample(i: Int, freq: Double): Double {
            val t = i.toDouble() / SAMPLE_RATE // FIXED
            return when (this) {
                SINE -> sin(2.0 * PI * freq * t)
                SQUARE -> if (sin(2.0 * PI * freq * t) >= 0) 1.0 else -1.0
                TRIANGLE -> 2.0 * abs(2.0 * (freq * t % 1.0) - 1.0) - 1.0
                SAWTOOTH -> 2.0 * (freq * t % 1.0) - 1.0
            }.coerceIn(-1.0, 1.0) // Clamp output
        }
    }

    var currentTrack: AudioTrack? = null

    fun playJingle(events: List<SoundEvent>, volume: Float = 1.0f, loop: Boolean = false) {
        if (events.isEmpty()) return

        // Calculate total duration considering offsets
        var currentTimeMs = 0
        var maxEndMs = 0

        // startSample, event
        val scheduledEvents = mutableListOf<Pair<Int, SoundEvent>>()

        for (event in events) {
            val startMs = currentTimeMs + event.offsetMs
            val endMs = startMs + event.durationMs

            val startSample = (startMs / 1000.0 * SAMPLE_RATE).toInt().coerceAtLeast(0)
            scheduledEvents.add(startSample to event)

            if (endMs > maxEndMs) maxEndMs = endMs
            currentTimeMs = endMs
        }

        val totalSamples = (maxEndMs / 1000.0 * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val mixBuffer = DoubleArray(totalSamples)

        for ((startSample, event) in scheduledEvents) {
            val samples = (event.durationMs / 1000.0 * SAMPLE_RATE).toInt()
            val attackSamples = (event.attackDurationMs / 1000.0 * SAMPLE_RATE).toInt()
            val releaseSamples = (event.releaseDurationMs / 1000.0 * SAMPLE_RATE).toInt()

            require(event.attackDurationMs + event.releaseDurationMs <= event.durationMs) {
                "Attack and release durations combined must be <= event duration"
            }

            for (i in 0 until samples) {
                var sampleSum = 0.0
                if (event.frequenciesHz.isNotEmpty()) {
                    for (freq in event.frequenciesHz) {
                        val baseFreq = freq + if (event.detuneHz != 0.0) {
                            if ((freq.hashCode() and 1) == 0) event.detuneHz else -event.detuneHz
                        } else 0.0

                        sampleSum += event.waveform.sample(i, baseFreq)

                        for ((multiple, amp) in event.harmonics) {
                            sampleSum += amp * event.waveform.sample(i, baseFreq * multiple)
                        }
                    }
                    sampleSum /= event.frequenciesHz.size
                }

                val amplitude = when {
                    i < attackSamples -> i.toDouble() / attackSamples
                    i > samples - releaseSamples -> (samples - i).toDouble() / releaseSamples
                    else -> 1.0
                }

                val sampleValue = (sampleSum * amplitude).coerceIn(-1.0, 1.0)
                val idx = startSample + i
                if (idx < mixBuffer.size) {
                    mixBuffer[idx] += sampleValue
                }
            }
        }

        // Normalize to prevent clipping and apply volume
        val maxAmp = mixBuffer.maxOf { abs(it) }.coerceAtLeast(1e-6)
        val volumeClamped = volume.coerceIn(0.0f, 1.0f)
        val pcm = ShortArray(totalSamples) { i ->
            (mixBuffer[i] / maxAmp * Short.MAX_VALUE * volumeClamped).toInt().toShort()
        }

        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            pcm.size * 2,
            AudioTrack.MODE_STATIC
        )

        currentTrack = track
        track.write(pcm, 0, pcm.size)

        if (loop) {
            track.setLoopPoints(0, pcm.size, -1)
        }

        track.play()
    }

    fun stop() {
        currentTrack?.pause()
    }
}
