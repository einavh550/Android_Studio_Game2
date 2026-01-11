package com.example.android_studio_game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.android_studio_game.R

object SoundManager {

    private lateinit var soundPool: SoundPool
    private var crashSoundId: Int = 0
    private var crashStreamId: Int = 0
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attributes)
            .build()

        crashSoundId = soundPool.load(context, R.raw.crash, 1)
        isInitialized = true
    }

    fun playCrash() {
        if (!isInitialized) return

        // stop existing sound to prevent delay
        if (crashStreamId != 0) {
            soundPool.stop(crashStreamId)
        }

        // restart
        crashStreamId = soundPool.play(
            crashSoundId,
            1f, // volume left
            1f, // volume right
            1,  // priority
            0,  // no loop
            1f  // normal speed
        )
    }

    fun release() {
        if (isInitialized) {
            soundPool.release()
            isInitialized = false
        }
    }
}
