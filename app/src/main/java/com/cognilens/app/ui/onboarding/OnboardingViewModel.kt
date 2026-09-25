package com.cognilens.app.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cognilens.app.data.preferences.UserProfileRepository
import com.cognilens.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserProfileRepository(application)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

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
        _userProfile.value = _userProfile.value.copy(bsmasScore = current.sum())
    }

    /** Saves baseline profile permanently to Preferences DataStore. */
    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveUserProfile(_userProfile.value)
            onSuccess()
        }
    }
}