package com.jdw.skillstestapp.transport

import android.content.Context
import com.jdw.skillstestapp.data.transport.ConnectionType
import com.jdw.skillstestapp.data.transport.DataTransport
import com.jdw.skillstestapp.data.transport.TransportState
import com.jdw.skillstestapp.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB CDC transport via Android USB Host API.
 * Connection is established automatically when a compatible device is detected via BroadcastReceiver.
 *
 * TODO: implement USB Host API (UsbManager, UsbDeviceConnection, BroadcastReceiver for attach/detach)
 */
@Singleton
class UsbTransport @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) : DataTransport {

    override val connectionType = ConnectionType.USB

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _receivedData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val receivedData: SharedFlow<ByteArray> = _receivedData.asSharedFlow()

    override fun connect() {
        // TODO: register BroadcastReceiver for UsbManager.ACTION_USB_DEVICE_ATTACHED
        //       request permission via UsbManager.requestPermission()
        //       open UsbDeviceConnection and start bulk-transfer read loop
        _state.value = TransportState.Error("USB not yet implemented")
    }

    override fun send(packet: ByteArray) {
        // TODO: UsbDeviceConnection.bulkTransfer(outEndpoint, packet, packet.size, timeout)
    }

    override fun disconnect() {
        // TODO: unregister BroadcastReceiver, close UsbDeviceConnection
        _state.value = TransportState.Disconnected
    }
}
