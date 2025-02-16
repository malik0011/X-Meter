package com.example.djmeter.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlin.math.abs
import kotlin.math.log10

class AudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )

    fun startRecording(onDecibelUpdate: (Float) -> Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val buffer = ShortArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    val amplitude = calculateAmplitude(buffer, read)
                    val db = 20 * log10(amplitude.coerceAtLeast(1f) / 32768.0) + 90
                    val normalizedDb = db.toFloat().coerceIn(0f, 120f)
                    onDecibelUpdate(normalizedDb)
                }
                Thread.sleep(100) // Update every 100ms
            }
        }.start()
    }

    private fun calculateAmplitude(buffer: ShortArray, readSize: Int): Float {
        var sum = 0f
        for (i in 0 until readSize) {
            sum += abs(buffer[i].toFloat())
        }
        return (sum / readSize).coerceAtLeast(1f)
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}