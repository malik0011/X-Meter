package com.example.djmeter.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.djmeter.utils.AudioRecorder
import com.example.djmeter.utils.PdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioRecorder = AudioRecorder(application.applicationContext)
    private val pdfGenerator = PdfGenerator()

    private val _decibelLevel = MutableStateFlow(0f)
    val decibelLevel: StateFlow<Float> = _decibelLevel

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _decibelReadings = MutableStateFlow<List<Float>>(emptyList())
    val decibelReadings: StateFlow<List<Float>> = _decibelReadings

    // Store session data
    private val _sessionReadings = MutableStateFlow<List<Float>>(emptyList())
    val sessionReadings: StateFlow<List<Float>> = _sessionReadings
    
    // Track if we have data to export
    private val _hasSessionData = MutableStateFlow(false)
    val hasSessionData: StateFlow<Boolean> = _hasSessionData

    private val _recordingTime = MutableStateFlow(0L)
    val recordingTime: StateFlow<Long> = _recordingTime
    
    private var timerJob: Job? = null

    fun startRecording() {
        if (_isRecording.value) return
        
        audioRecorder.startRecording { decibel ->
            viewModelScope.launch {
                _decibelLevel.value = decibel
                val currentReadings = _decibelReadings.value.toMutableList()
                if (currentReadings.size >= 50) {
                    currentReadings.removeAt(0)
                }
                currentReadings.add(decibel)
                _decibelReadings.value = currentReadings
            }
        }
        _isRecording.value = true
        startTimer()
    }

    private fun startTimer() {
        _recordingTime.value = 0L
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // Update every second
                _recordingTime.value += 1
            }
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        
        audioRecorder.stopRecording()
        _isRecording.value = false
        _decibelLevel.value = 0f
        timerJob?.cancel()
        _recordingTime.value = 0L
        
        // Save the readings to session data
        _sessionReadings.value = _decibelReadings.value.toList()
        _hasSessionData.value = _sessionReadings.value.isNotEmpty()
        
        // Clear current readings
        _decibelReadings.value = emptyList()
    }

    fun exportGraphToPdf(context: Context): String? {
        return if (_sessionReadings.value.isNotEmpty()) {
            pdfGenerator.generateDecibelGraphPdf(context, _sessionReadings.value)
        } else {
            null
        }
    }

    fun clearSessionData() {
        _sessionReadings.value = emptyList()
        _hasSessionData.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
        clearSessionData()
    }
}