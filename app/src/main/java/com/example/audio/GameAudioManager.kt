package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect {
    EAT_FRUIT,
    PICKUP_COIN,
    PICKUP_GEM,
    POWER_UP,
    KILL_ENEMY,
    SHIELD_BLOCK,
    GAME_OVER,
    BUTTON_TAP,
    REWARD_CLAIM
}

/**
 * Lightweight procedural PCM sound synthesizer using AudioTrack so the game
 * has crisp cartoon sound effects with zero external audio asset dependencies.
 */
class GameAudioManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    var soundEnabled: Boolean = true
    private var lastEatMs = 0L

    fun play(effect: SoundEffect) {
        if (!soundEnabled) return
        val now = System.currentTimeMillis()
        if (effect == SoundEffect.EAT_FRUIT && now - lastEatMs < 75L) return
        if (effect == SoundEffect.EAT_FRUIT) lastEatMs = now

        scope.launch {
            runCatching {
                when (effect) {
                    SoundEffect.EAT_FRUIT -> playChirp(520.0, 740.0, 70, 0.18f)
                    SoundEffect.PICKUP_COIN -> playTwoTone(880.0, 1175.0, 55, 65, 0.22f)
                    SoundEffect.PICKUP_GEM -> playTwoTone(988.0, 1568.0, 65, 95, 0.24f)
                    SoundEffect.POWER_UP -> playChirp(440.0, 1180.0, 180, 0.25f)
                    SoundEffect.KILL_ENEMY -> playTwoTone(660.0, 990.0, 70, 110, 0.26f)
                    SoundEffect.SHIELD_BLOCK -> playChirp(620.0, 320.0, 130, 0.25f)
                    SoundEffect.GAME_OVER -> playChirp(380.0, 140.0, 320, 0.25f)
                    SoundEffect.BUTTON_TAP -> playChirp(620.0, 700.0, 40, 0.14f)
                    SoundEffect.REWARD_CLAIM -> playTwoTone(784.0, 1318.0, 80, 140, 0.25f)
                }
            }
        }
    }

    private fun playChirp(startHz: Double, endHz: Double, durationMs: Int, volume: Float) {
        val sampleRate = 22050
        val count = (sampleRate * durationMs) / 1000
        if (count <= 0) return
        val pcm = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-3.2 * t) * (1.0 - exp(-25.0 * t))
            pcm[i] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        writePcm(pcm, sampleRate)
    }

    private fun playTwoTone(hz1: Double, hz2: Double, ms1: Int, ms2: Int, volume: Float) {
        val sampleRate = 22050
        val c1 = (sampleRate * ms1) / 1000
        val c2 = (sampleRate * ms2) / 1000
        val pcm = ShortArray(c1 + c2)
        for (i in 0 until c1) {
            val t = i.toDouble() / c1
            val env = exp(-2.5 * t)
            pcm[i] = (sin(2.0 * PI * hz1 * i / sampleRate) * env * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        for (i in 0 until c2) {
            val t = i.toDouble() / c2
            val env = exp(-3.0 * t)
            pcm[c1 + i] = (sin(2.0 * PI * hz2 * i / sampleRate) * env * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        writePcm(pcm, sampleRate)
    }

    private fun writePcm(pcm: ShortArray, sampleRate: Int) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcm.size * 2)

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf,
                AudioTrack.MODE_STATIC
            )
        }
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(((pcm.size * 1000L) / sampleRate) + 25L)
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }
}
