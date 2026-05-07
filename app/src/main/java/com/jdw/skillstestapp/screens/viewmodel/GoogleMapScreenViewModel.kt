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
        getAllImages()
        fetchImagesToDb()
    }

    fun fetchImagesToDb() {
        viewModelScope.launch(Dispatchers.IO) {
            mapsRepository.fetchImages()
            getAllImages()
        }
    }

    fun getAllImages() {
        viewModelScope.launch(Dispatchers.IO) {
            val images = mapsRepository.getAllImages()
            val existing = images.filter { mapsRepository.imageIsExist(it.imageDataPath) }
            (images - existing.toSet()).forEach { mapsRepository.deleteUserImage(it) }
            _userImages.value = existing.sortedByDescending { it.imageDateTaken }
        }
    }
}
