package com.example.djmeter.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.djmeter.utils.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioRecorder = AudioRecorder(application.applicationContext)

    private val _decibelLevel = MutableStateFlow(0f)
    val decibelLevel: StateFlow<Float> = _decibelLevel

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _decibelReadings = MutableStateFlow<List<Float>>(emptyList())
    val decibelReadings: StateFlow<List<Float>> = _decibelReadings

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
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        
        audioRecorder.stopRecording()
        _isRecording.value = false
        _decibelLevel.value = 0f
        _decibelReadings.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}