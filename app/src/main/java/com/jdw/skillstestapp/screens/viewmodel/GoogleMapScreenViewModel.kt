package com.jdw.skillstestapp.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdw.skillstestapp.data.model.UserImg
import com.jdw.skillstestapp.repository.GoogleMapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleMapScreenViewModel @Inject constructor(
    private val mapsRepository: GoogleMapsRepository,
) : ViewModel() {

    private val _userImages: MutableStateFlow<List<UserImg>> = MutableStateFlow(emptyList())
    val userImages: StateFlow<List<UserImg>> = _userImages.asStateFlow()

    init {
        viewModelScope.launch {
            mapsRepository.getAllImages().collect { _userImages.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            mapsRepository.syncDeletedImages()
            mapsRepository.fetchImages()
        }
    }
}
