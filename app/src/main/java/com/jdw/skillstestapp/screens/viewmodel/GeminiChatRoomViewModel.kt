package com.jdw.skillstestapp.screens.viewmodel

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.jdw.skillstestapp.data.model.ChatItem
import com.jdw.skillstestapp.data.model.ChatMessage
import com.jdw.skillstestapp.data.model.GeminiIntent
import com.jdw.skillstestapp.data.model.SearchCard
import com.jdw.skillstestapp.repository.ChatRepository
import com.jdw.skillstestapp.repository.PlacesRepository
import com.jdw.skillstestapp.utils.getTimeStamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class GeminiChatRoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val generativeModel: GenerativeModel,
    private val placesRepository: PlacesRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
) : ViewModel() {

    private val gson = Gson()

    private val _chatItems = MutableStateFlow<List<ChatItem>>(emptyList())
    val chatItems: StateFlow<List<ChatItem>> = _chatItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatRepository.getAllMessage()
            if (messages.isEmpty()) {
                val welcome = ChatMessage(
                    sender = "Gemini",
                    message = "안녕하세요! 무엇이든 물어보세요.\n\n'근처 맛집 찾아줘', '약국 어딨어?' 처럼 주변 장소 검색도 가능해요!",
                    timestamp = getTimeStamp()
                )
                chatRepository.insertMessage(welcome)
                _chatItems.value = listOf(ChatItem.Message(welcome))
            } else {
                _chatItems.value = messages.map { ChatItem.Message(it) }
            }
        }
    }

    fun sendAndReceiveMessage(userMessage: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.insertMessage(userMessage)
            appendItem(ChatItem.Message(userMessage))
            _isLoading.value = true

            try {
                val responseText = generativeModel.generateContent(userMessage.message).text ?: ""
                val intent = parseIntent(responseText)

                when (intent.intent) {
                    "PLACE_SEARCH" -> handlePlaceSearch(intent)
                    else -> {
                        val aiMsg = ChatMessage(
                            sender = "Gemini",
                            message = intent.message.ifEmpty { responseText },
                            timestamp = getTimeStamp()
                        )
                        chatRepository.insertMessage(aiMsg)
                        appendItem(ChatItem.Message(aiMsg))
                    }
                }
            } catch (e: Exception) {
                val errMsg = ChatMessage(
                    sender = "Gemini",
                    message = "오류가 발생했습니다: ${e.message ?: "Unknown error"}",
                    timestamp = getTimeStamp()
                )
                chatRepository.insertMessage(errMsg)
                appendItem(ChatItem.Message(errMsg))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseIntent(text: String): GeminiIntent {
        return try {
            val cleaned = text
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            gson.fromJson(cleaned, GeminiIntent::class.java)
        } catch (e: Exception) {
            GeminiIntent(intent = "GENERAL", message = text)
        }
    }

    // ── 장소 검색 핸들러 ─────────────────────────────────────────────────────
    // 새 검색 타입 추가 시: handleXxxSearch() 형태로 동일 패턴 복사, Place→SearchCard 매핑만 교체

    @SuppressLint("MissingPermission")
    private suspend fun handlePlaceSearch(intent: GeminiIntent) {
        val announceMsg = ChatMessage(
            sender = "Gemini",
            message = intent.message,
            timestamp = getTimeStamp()
        )
        chatRepository.insertMessage(announceMsg)
        appendItem(ChatItem.Message(announceMsg))

        val location = getCurrentLocation()
        if (location == null) {
            appendErrorMsg("위치 정보를 가져올 수 없어요. 위치 권한을 확인해주세요.")
            return
        }

        placesRepository.searchNearbyPlaces(
            location = location,
            radiusMeters = intent.radiusMeters.toDouble(),
            maxCount = 5,
            keyword = intent.keyword
        ).onSuccess { places ->
            if (places.isEmpty()) {
                appendErrorMsg("주변에 검색된 장소가 없어요.")
                return@onSuccess
            }

            // Place → SearchCard 변환
            val cards = places.map { place ->
                SearchCard(
                    id = place.id ?: "",
                    title = place.name ?: "알 수 없음",
                    subtitle = place.address,
                    badge = place.rating?.let { "⭐ ${"%.1f".format(it)}" },
                    thumbnail = null,
                    latLng = place.latLng,
                    actionUrl = null
                )
            }

            val resultItem = ChatItem.CardResult(
                id = getTimeStamp(),
                query = intent.keyword ?: "장소",
                cards = cards
            )
            appendItem(resultItem)

            // 썸네일 비동기 로딩
            places.forEachIndexed { index, place ->
                val meta = place.photoMetadatas?.firstOrNull() ?: return@forEachIndexed
                viewModelScope.launch(Dispatchers.IO) {
                    placesRepository.fetchPlacePhoto(meta, maxWidth = 200, maxHeight = 200)
                        .onSuccess { bitmap -> updateThumbnail(resultItem.id, index, bitmap) }
                }
            }
        }.onFailure {
            appendErrorMsg("장소 검색 중 오류가 발생했어요: ${it.message}")
        }
    }

    // ── 공용 유틸 ────────────────────────────────────────────────────────────

    private fun appendItem(item: ChatItem) {
        _chatItems.value = _chatItems.value + item
    }

    private suspend fun appendErrorMsg(text: String) {
        val msg = ChatMessage(sender = "Gemini", message = text, timestamp = getTimeStamp())
        chatRepository.insertMessage(msg)
        appendItem(ChatItem.Message(msg))
    }

    private fun updateThumbnail(resultId: Long, cardIndex: Int, bitmap: Bitmap) {
        _chatItems.value = _chatItems.value.map { item ->
            if (item is ChatItem.CardResult && item.id == resultId) {
                val updated = item.cards.toMutableList()
                if (cardIndex < updated.size) {
                    updated[cardIndex] = updated[cardIndex].copy(thumbnail = bitmap)
                }
                item.copy(cards = updated)
            } else item
        }
    }

    fun deleteMessage(chatMessage: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.deleteMessage(chatMessage)
            _chatItems.value = _chatItems.value.filter { item ->
                !(item is ChatItem.Message && item.chatMessage.id == chatMessage.id)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): LatLng? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                cont.resume(loc?.let { LatLng(it.latitude, it.longitude) })
            }
            .addOnFailureListener { cont.resume(null) }
    }
}
