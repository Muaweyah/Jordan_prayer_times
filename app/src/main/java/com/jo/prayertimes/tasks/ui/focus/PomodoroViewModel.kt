package com.jo.prayertimes.tasks.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class PomodoroPhase { WORK, BREAK, IDLE }

class PomodoroViewModel : ViewModel() {
    private val workSeconds = 25 * 60
    private val breakSeconds = 5 * 60

    private val _secondsLeft = MutableStateFlow(workSeconds)
    val secondsLeft: StateFlow<Int> = _secondsLeft

    private val _phase = MutableStateFlow(PomodoroPhase.IDLE)
    val phase: StateFlow<PomodoroPhase> = _phase

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private var job: Job? = null

    fun start() {
        if (_phase.value == PomodoroPhase.IDLE) {
            _phase.value = PomodoroPhase.WORK
            _secondsLeft.value = workSeconds
        }
        _isRunning.value = true
        job?.cancel()
        job = viewModelScope.launch {
            while (_secondsLeft.value > 0 && _isRunning.value) {
                delay(1000)
                _secondsLeft.value -= 1
            }
            if (_secondsLeft.value == 0) {
                if (_phase.value == PomodoroPhase.WORK) {
                    _phase.value = PomodoroPhase.BREAK
                    _secondsLeft.value = breakSeconds
                } else {
                    _phase.value = PomodoroPhase.IDLE
                    _secondsLeft.value = workSeconds
                    _isRunning.value = false
                }
            }
        }
    }

    fun pause() {
        _isRunning.value = false
        job?.cancel()
    }

    fun reset() {
        job?.cancel()
        _isRunning.value = false
        _phase.value = PomodoroPhase.IDLE
        _secondsLeft.value = workSeconds
    }
}
