package com.jdw.skillstestapp.data.model

sealed class ChatItem {
    data class Message(val chatMessage: ChatMessage) : ChatItem()
    data class CardResult(
        val id: Long = System.currentTimeMillis(),
        val query: String,
        val cards: List<SearchCard>
    ) : ChatItem()
}
