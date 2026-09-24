package com.cognilens.app.ui.onboarding

import androidx.lifecycle.ViewModel
import com.cognilens.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

class OnboardingViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // BSMAS Answers (6 questions, default score 3 per item)
    private val _bsmasAnswers = MutableStateFlow(IntArray(6) { 3 })
    val bsmasAnswers: StateFlow<IntArray> = _bsmasAnswers.asStateFlow()

    fun updateWakeUpTime(time: LocalTime) {
        _userProfile.value = _userProfile.value.copy(wakeUpTime = time)
    }

    fun updateSleepTime(time: LocalTime) {
        _userProfile.value = _userProfile.value.copy(sleepTime = time)
    }

    fun updateBsmasAnswer(index: Int, score: Int) {
        val current = _bsmasAnswers.value.copyOf()
        current[index] = score
        _bsmasAnswers.value = current

        // Recalculate total BSMAS score (6 to 30)
        val totalScore = current.sum()
        _userProfile.value = _userProfile.value.copy(bsmasScore = totalScore)
    }
}