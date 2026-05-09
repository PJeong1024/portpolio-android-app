package com.jdw.skillstestapp.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdw.skillstestapp.data.model.UserImg
import com.jdw.skillstestapp.data.model.packet.ImageListItem
import com.jdw.skillstestapp.packet.PacketBuilder
import com.jdw.skillstestapp.repository.GoogleMapsRepository
import com.jdw.skillstestapp.transport.TransportManager
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
    private val transportManager: TransportManager
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

    /**
     * Sends image metadata for clicked marker(s) to all connected transports.
     * Single marker click → list of 1 item. Cluster click → full cluster list.
     */
    fun sendMarkerData(images: List<UserImg>) {
        val items = images.map { img ->
            ImageListItem(
                imageID = img.imageID,
                imageDisplayName = img.imageDisplayName,
                imageLat = img.imageLat,
                imageLong = img.imageLong
            )
        }
        val packet = PacketBuilder.buildImageList(items)
        transportManager.sendToAll(packet)
    }
}
