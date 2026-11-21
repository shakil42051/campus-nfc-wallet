package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.di.AppContainer

class ViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                cardRepository = appContainer.cardRepository,
                settingsRepository = appContainer.settingsRepository,
                attendanceRepository = appContainer.attendanceRepository,
                campusServerSyncService = appContainer.campusServerSyncService,
                roleAccessManager = appContainer.roleAccessManager,
                feedbackManager = appContainer.feedbackManager,
                backupService = appContainer.backupService,
                restoreService = appContainer.restoreService,
                backupHistoryRepository = appContainer.backupHistoryRepository,
                backupFileGateway = appContainer.backupFileGateway
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
