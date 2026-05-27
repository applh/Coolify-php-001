package com.example.cameraxapp.roguelike

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class RoguelikeAudioEngine {
    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        } catch (e: Exception) {
            Log.e("RoguelikeAudio", "Could not initialize standard ToneGenerator", e)
        }
    }

    fun playMove() {
        playTone(ToneGenerator.TONE_CDMA_PIP, 45)
    }

    fun playAttack() {
        playTone(ToneGenerator.TONE_PROP_ACK, 80)
    }

    fun playHit() {
        playTone(ToneGenerator.TONE_PROP_BEEP2, 110)
    }

    fun playItem() {
        playTone(ToneGenerator.TONE_CDMA_CONFIRM, 120)
    }

    fun playCast() {
        playTone(ToneGenerator.TONE_PROP_PROMPT, 150)
    }

    fun playLevelUp() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 220)
            Thread {
                try {
                    Thread.sleep(180)
                    toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 220)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playDeath() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            Thread {
                try {
                    Thread.sleep(300)
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGen?.startTone(toneType, durationMs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            toneGen?.release()
            toneGen = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
